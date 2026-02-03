package org.refit.refitbackend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.entity.EmailVerification;
import org.refit.refitbackend.domain.auth.entity.EmailVerificationStatus;
import org.refit.refitbackend.domain.auth.repository.EmailVerificationRepository;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.refit.refitbackend.domain.master.entity.Job;
import org.refit.refitbackend.domain.master.entity.Skill;
import org.refit.refitbackend.domain.master.repository.CareerLevelRepository;
import org.refit.refitbackend.domain.master.repository.EmailDomainRepository;
import org.refit.refitbackend.domain.master.repository.JobRepository;
import org.refit.refitbackend.domain.master.repository.SkillRepository;
import org.refit.refitbackend.domain.user.entity.*;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.refit.refitbackend.domain.user.entity.enums.UserStatus;
import org.refit.refitbackend.domain.user.repository.*;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final CareerLevelRepository careerLevelRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final EmailDomainRepository emailDomainRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserJobRepository userJobRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    @Transactional
    public User signup(AuthReq.SignUp signUpDto) {
        User existing = userRepository.findByOauthProviderAndOauthId(signUpDto.oauthProvider(), signUpDto.oauthId())
                .orElse(null);

        validateSignUp(signUpDto, existing);

        CareerLevel careerLevel = getCareerLevel(signUpDto.careerLevelId());

        if (existing != null && existing.isDeleted()) {
            existing.restoreActive();
            existing.updateUserType(signUpDto.userType());
            existing.updateCareerLevel(careerLevel);
            existing.updateProfile(signUpDto.email(), signUpDto.nickname());
            existing.updateIntroduction(signUpDto.introduction());
            existing.clearProfileImageUrl();

            userJobRepository.deleteByUser_Id(existing.getId());
            userSkillRepository.deleteByUser_Id(existing.getId());
            mapJobs(existing, signUpDto.jobIds());
            mapSkills(existing, signUpDto.skills());

            if (existing.getUserType() != UserType.EXPERT) {
                if (existing.getExpertProfile() != null) {
                    expertProfileRepository.deleteById(existing.getId());
                    existing.clearExpertProfile();
                }
            } else {
                createExpertProfileIfNeeded(existing, signUpDto);
            }
            return existing;
        }

        User user = userRepository.save(createUser(signUpDto, careerLevel));
        mapJobs(user, signUpDto.jobIds());
        mapSkills(user, signUpDto.skills());
        createExpertProfileIfNeeded(user, signUpDto);
        return user;
    }

    @Transactional
    public User restore(AuthReq.Restore request) {
        User user = userRepository.findByOauthProviderAndOauthId(request.oauthProvider(), request.oauthId())
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        if (!user.isDeleted()) {
            throw new CustomException(ExceptionType.ACCOUNT_RESTORE_NOT_ALLOWED);
        }

        validateRestore(request);

        user.restoreActive();
        user.updateProfile(request.email(), request.nickname());
        user.clearProfileImageUrl();

        return user;
    }


    private User createUser(AuthReq.SignUp signUp, CareerLevel careerLevel) {
        return User.builder()
                .oauthProvider(signUp.oauthProvider())
                .oauthId(signUp.oauthId())
                .email(signUp.email())
                .nickname(signUp.nickname())
                .userType(signUp.userType())
                .careerLevel(careerLevel)
                .introduction(signUp.introduction())
                .build();
    }


    private void mapJobs(User user, List<Long> jobIds) {

        if (jobIds == null || jobIds.isEmpty()) return;

        List<UserJob> userJobs = jobIds.stream()
                .map(jobId -> {
                    Job job = jobRepository.findById(jobId)
                            .orElseThrow(() -> new CustomException(ExceptionType.JOB_NOT_FOUND));

                    return UserJob.builder()
                            .user(user)
                            .job(job)
                            .build();
                })
                .toList();

        userJobRepository.saveAll(userJobs);
    }

    private void mapSkills(User user, List<AuthReq.SkillRequest> skills) {

        if (skills == null || skills.isEmpty()) return;

        List<UserSkill> userSkills = skills.stream()
                .map(req -> {
                    Skill skill = skillRepository.findById(req.skillId())
                            .orElseThrow(() -> new CustomException(ExceptionType.SKILL_NOT_FOUND));

                    return UserSkill.builder()
                            .user(user)
                            .skill(skill)
                            .displayOrder(req.displayOrder())
                            .build();
                })
                .toList();

        userSkillRepository.saveAll(userSkills);
    }

    private void createExpertProfileIfNeeded(User user, AuthReq.SignUp signUp) {
        if (user.getUserType() != UserType.EXPERT) {
            return;
        }
        if (user.getExpertProfile() != null) {
            return;
        }
        String companyEmail = normalizeEmail(signUp.companyEmail());
        String companyName = resolveCompanyName(signUp.companyName(), companyEmail);

        LocalDateTime verifiedAt = null;
        if (companyEmail != null) {
            verifiedAt = emailVerificationRepository.findTopByEmailOrderByIdDesc(companyEmail)
                    .filter(v -> v.getStatus() == EmailVerificationStatus.VERIFIED)
                    .map(v -> v.getVerifiedAt() != null ? v.getVerifiedAt() : v.getUpdatedAt())
                    .orElse(null);
        }

        ExpertProfile profile = ExpertProfile.create(user, companyName, companyEmail);
        if (verifiedAt != null) {
            profile.markVerified(verifiedAt);
        }
        expertProfileRepository.save(profile);
    }

    private String resolveCompanyName(String companyName, String companyEmail) {
        if (companyName != null && !companyName.isBlank()) {
            return companyName;
        }
        if (companyEmail == null) {
            return null;
        }
        String domain = extractDomain(companyEmail);
        if (domain == null) {
            return null;
        }
        return emailDomainRepository.findById(domain)
                .map(d -> d.getCompanyName())
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private String extractDomain(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return null;
        }
        return email.substring(at + 1).toLowerCase();
    }


    private CareerLevel getCareerLevel(Long careerLevelId) {
        return careerLevelRepository.findById(careerLevelId)
                .orElseThrow(() -> new CustomException(ExceptionType.CAREER_LEVEL_NOT_FOUND));
    }


    private void validateSignUp(AuthReq.SignUp signUp, User existing) {
        if (existing != null && !existing.isDeleted()) {
            throw new CustomException(ExceptionType.OAUTH_DUPLICATE);
        }
        if (userRepository.existsByEmailAndStatus(signUp.email(), UserStatus.ACTIVE)) {
            throw new CustomException(ExceptionType.EMAIL_DUPLICATE);
        }
        if (userRepository.existsByNicknameAndStatus(signUp.nickname(), UserStatus.ACTIVE)) {
            throw new CustomException(ExceptionType.NICKNAME_DUPLICATE);
        }
    }

    private void validateRestore(AuthReq.Restore request) {
        if (userRepository.existsByEmailAndStatus(request.email(), UserStatus.ACTIVE)) {
            throw new CustomException(ExceptionType.EMAIL_DUPLICATE);
        }
        if (userRepository.existsByNicknameAndStatus(request.nickname(), UserStatus.ACTIVE)) {
            throw new CustomException(ExceptionType.NICKNAME_DUPLICATE);
        }
    }

}
