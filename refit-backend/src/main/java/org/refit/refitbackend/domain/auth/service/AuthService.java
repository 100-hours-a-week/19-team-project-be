package org.refit.refitbackend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.refit.refitbackend.domain.master.entity.Job;
import org.refit.refitbackend.domain.master.entity.Skill;
import org.refit.refitbackend.domain.master.repository.CareerLevelRepository;
import org.refit.refitbackend.domain.master.repository.JobRepository;
import org.refit.refitbackend.domain.master.repository.SkillRepository;
import org.refit.refitbackend.domain.user.entity.*;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.refit.refitbackend.domain.user.repository.*;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final CareerLevelRepository careerLevelRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserJobRepository userJobRepository;
    private final ExpertProfileRepository expertProfileRepository;

    @Transactional
    public User signup(AuthReq.SignUp signUpDto) {
        validateSignUp(signUpDto);

        CareerLevel careerLevel = getCareerLevel(signUpDto.careerLevelId());
        User user = userRepository.save(createUser(signUpDto, careerLevel));

        mapJobs(user, signUpDto.jobIds());
        mapSkills(user, signUpDto.skills());
        createExpertProfileIfNeeded(user);
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

    private void createExpertProfileIfNeeded(User user) {
        if (user.getUserType() != UserType.EXPERT) {
            return;
        }
        if (user.getExpertProfile() != null) {
            return;
        }
        String companyName = "TempCompany";
        String companyEmail = "user" + user.getId() + "@tempcorp.com";
        expertProfileRepository.save(ExpertProfile.create(user, companyName, companyEmail));
    }


    private CareerLevel getCareerLevel(Long careerLevelId) {
        return careerLevelRepository.findById(careerLevelId)
                .orElseThrow(() -> new CustomException(ExceptionType.CAREER_LEVEL_NOT_FOUND));
    }


    private void validateSignUp(AuthReq.SignUp signUp) {
        // OAuth 중복
        if (userRepository.existsByOauthProviderAndOauthId(signUp.oauthProvider(), signUp.oauthId())) throw new CustomException(ExceptionType.OAUTH_DUPLICATE);
        // Email 중복
        if (userRepository.existsByEmail(signUp.email())) throw new CustomException(ExceptionType.EMAIL_DUPLICATE);
        // Nickname 중복
        if (userRepository.existsByNickname(signUp.nickname())) throw new CustomException(ExceptionType.NICKNAME_DUPLICATE);
    }

}