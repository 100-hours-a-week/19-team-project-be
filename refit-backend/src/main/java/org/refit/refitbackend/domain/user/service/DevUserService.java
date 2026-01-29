package org.refit.refitbackend.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.entity.EmailVerification;
import org.refit.refitbackend.domain.auth.entity.EmailVerificationStatus;
import org.refit.refitbackend.domain.auth.repository.EmailVerificationRepository;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.user.dto.DevUserReq;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevUserService {

    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    @Transactional
    public void changeUserType(Long userId, DevUserReq.ChangeUserType request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        UserType targetType = parseUserType(request.userType());
        user.updateUserType(targetType);

        if (targetType == UserType.EXPERT) {
            if (user.getExpertProfile() == null) {
                String companyName = request.companyName() != null ? request.companyName() : "TestCompany";
                String companyEmail = request.companyEmail();
                if (companyEmail == null || companyEmail.isBlank()) {
                    throw new CustomException(ExceptionType.EMAIL_VERIFICATION_REQUIRED);
                }

                EmailVerification latest = emailVerificationRepository
                        .findTopByEmailOrderByIdDesc(companyEmail)
                        .orElseThrow(() -> new CustomException(ExceptionType.EMAIL_VERIFICATION_REQUIRED));

                if (latest.getStatus() != EmailVerificationStatus.VERIFIED) {
                    throw new CustomException(ExceptionType.EMAIL_VERIFICATION_NOT_VERIFIED);
                }

                expertProfileRepository.save(ExpertProfile.create(user, companyName, companyEmail));
            }
        } else {
            if (user.getExpertProfile() != null) {
                expertProfileRepository.deleteById(userId);
                user.clearExpertProfile();
            }
        }
    }

    private UserType parseUserType(String value) {
        try {
            return UserType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionType.USER_TYPE_INVALID);
        }
    }
}
