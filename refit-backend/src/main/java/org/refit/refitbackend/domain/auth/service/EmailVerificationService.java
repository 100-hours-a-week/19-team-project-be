package org.refit.refitbackend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationRes;
import org.refit.refitbackend.domain.auth.entity.EmailVerification;
import org.refit.refitbackend.domain.auth.entity.EmailVerificationStatus;
import org.refit.refitbackend.domain.auth.repository.EmailVerificationRepository;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.master.repository.EmailDomainRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_SENDS_PER_WINDOW = 3;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Set<String> FREE_EMAIL_DOMAINS = Set.of(
//            "gmail.com", "naver.com", "daum.net", "kakao.com", "hanmail.net",
            "hotmail.com", "outlook.com", "live.com", "yahoo.com", "icloud.com",
            "proton.me", "protonmail.com"
    );

    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "mailinator.com", "guerrillamail.com", "10minutemail.com", "tempmail.com"
    );

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final EmailDomainRepository emailDomainRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public EmailVerificationRes.Send sendVerificationCode(Long userId, String email) {
        validateEmail(email);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        EmailVerification latest = emailVerificationRepository
                .findTopByUserIdAndEmailOrderByIdDesc(userId, email)
                .orElse(null);

        return sendVerificationCodeInternal(user, email, latest);
    }

    @Transactional
    public EmailVerificationRes.Send sendVerificationCodePublic(String email) {
        validateEmail(email);

        EmailVerification latest = emailVerificationRepository
                .findTopByEmailOrderByIdDesc(email)
                .orElse(null);

        return sendVerificationCodeInternal(null, email, latest);
    }

    private EmailVerificationRes.Send sendVerificationCodeInternal(User user, String email, EmailVerification latest) {
        LocalDateTime now = LocalDateTime.now();
        int sentCount = 1;
        if (latest != null) {
            if (latest.getStatus() == EmailVerificationStatus.VERIFIED) {
                throw new CustomException(ExceptionType.EMAIL_ALREADY_VERIFIED);
            }
            LocalDateTime lastSentAt = latest.getUpdatedAt() != null ? latest.getUpdatedAt() : latest.getCreatedAt();
            if (lastSentAt != null && Duration.between(lastSentAt, now).compareTo(RATE_LIMIT_WINDOW) < 0) {
                if (latest.getSentCount() >= MAX_SENDS_PER_WINDOW) {
                    long retryAfter = RATE_LIMIT_WINDOW.minus(Duration.between(lastSentAt, now)).getSeconds();
                    throw new CustomException(
                            ExceptionType.EMAIL_VERIFICATION_RATE_LIMIT,
                            new EmailVerificationRes.RateLimit(retryAfter)
                    );
                }
                sentCount = latest.getSentCount() + 1;
            }
        }

        String code = generateCode();
        LocalDateTime expiresAt = now.plus(CODE_TTL);

        EmailVerification verification;
        if (latest == null) {
            verification = EmailVerification.builder()
                    .user(user)
                    .email(email)
                    .code(code)
                    .sentCount(sentCount)
                    .expiresAt(expiresAt)
                    .build();
        } else {
            verification = latest;
            verification.updateForResend(code, sentCount, expiresAt);
        }
        emailVerificationRepository.save(verification);

        sendEmail(email, code);

        int remainingAttempts = Math.max(0, MAX_SENDS_PER_WINDOW - sentCount);
        return new EmailVerificationRes.Send(email, expiresAt, sentCount, remainingAttempts);
    }

    @Transactional
    public EmailVerificationRes.Verify verifyCode(Long userId, String email, String code) {
        validateEmail(email);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        EmailVerification latest = emailVerificationRepository
                .findTopByUserIdAndEmailOrderByIdDesc(userId, email)
                .orElseThrow(() -> new CustomException(ExceptionType.VERIFICATION_CODE_INVALID));

        LocalDateTime now = LocalDateTime.now();
        if (latest.getExpiresAt().isBefore(now)) {
            latest.markExpired();
            throw new CustomException(ExceptionType.VERIFICATION_CODE_EXPIRED);
        }

        if (!latest.getCode().equals(code)) {
            throw new CustomException(ExceptionType.VERIFICATION_CODE_MISMATCH);
        }

        latest.markVerified(now);
        promoteExpertIfNeeded(user, email, now);
        return new EmailVerificationRes.Verify(email, now);
    }

    @Transactional
    public EmailVerificationRes.Verify verifyCodePublic(String email, String code) {
        validateEmail(email);

        EmailVerification latest = emailVerificationRepository
                .findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new CustomException(ExceptionType.VERIFICATION_CODE_INVALID));

        LocalDateTime now = LocalDateTime.now();
        if (latest.getExpiresAt().isBefore(now)) {
            latest.markExpired();
            throw new CustomException(ExceptionType.VERIFICATION_CODE_EXPIRED);
        }

        if (!latest.getCode().equals(code)) {
            throw new CustomException(ExceptionType.VERIFICATION_CODE_MISMATCH);
        }

        latest.markVerified(now);
        userRepository.findByEmail(email)
                .ifPresent(user -> promoteExpertIfNeeded(user, email, now));
        return new EmailVerificationRes.Verify(email, now);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomException(ExceptionType.EMAIL_FORMAT_INVALID);
        }

        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (!domain.contains(".")) {
            throw new CustomException(ExceptionType.EMAIL_DOMAIN_NOT_ALLOWED);
        }
        if (DISPOSABLE_EMAIL_DOMAINS.contains(domain)) {
            throw new CustomException(ExceptionType.EMAIL_DOMAIN_NOT_ALLOWED);
        }
        if (FREE_EMAIL_DOMAINS.contains(domain)) {
            throw new CustomException(ExceptionType.EMAIL_NOT_COMPANY_EMAIL);
        }
        if (!emailDomainRepository.existsById(domain)) {
            throw new CustomException(ExceptionType.EMAIL_DOMAIN_NOT_ALLOWED);
        }
    }

    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Re-fit] Email Verification Code");
        message.setText("Your verification code is " + code + ". It expires in 5 minutes.");
        mailSender.send(message);
    }

    private void promoteExpertIfNeeded(User user, String email, LocalDateTime verifiedAt) {
        if (user.getUserType() != UserType.EXPERT) {
            user.updateUserType(UserType.EXPERT);
        }

        ExpertProfile profile = user.getExpertProfile();
        if (profile == null) {
            String companyName = resolveCompanyName(email);
            ExpertProfile newProfile = ExpertProfile.create(user, companyName, email);
            newProfile.markVerified(verifiedAt);
            expertProfileRepository.save(newProfile);
            return;
        }

        profile.markVerified(verifiedAt);
    }

    private String resolveCompanyName(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return null;
        }
        return emailDomainRepository.findById(domain)
                .map(d -> d.getCompanyName())
                .orElse(null);
    }

    private String extractDomain(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return null;
        }
        return email.substring(at + 1).toLowerCase();
    }
}
