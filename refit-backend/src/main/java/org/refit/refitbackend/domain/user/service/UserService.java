package org.refit.refitbackend.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.refit.refitbackend.domain.master.entity.Job;
import org.refit.refitbackend.domain.master.entity.Skill;
import org.refit.refitbackend.domain.master.repository.CareerLevelRepository;
import org.refit.refitbackend.domain.master.repository.JobRepository;
import org.refit.refitbackend.domain.master.repository.SkillRepository;
import org.refit.refitbackend.domain.user.dto.UserReq;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.UserJob;
import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CareerLevelRepository careerLevelRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final ExpertProfileRepository expertProfileRepository;

    public UserRes.Detail getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        return UserRes.Detail.from(user);
    }

    public UserRes.Me getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        return UserRes.Me.from(user);
    }

    public UserRes.ExpertVerificationStatus getExpertVerificationStatus(Long userId) {
        ExpertProfile profile = expertProfileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return new UserRes.ExpertVerificationStatus(false, null, null, null);
        }
        return new UserRes.ExpertVerificationStatus(
                profile.isVerified(),
                profile.getVerifiedAt(),
                profile.getCompanyName(),
                profile.getCompanyEmail()
        );
    }

    @Transactional
    public UserRes.Me updateMe(Long userId, UserReq.UpdateMe request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        updateNicknameIfPresent(user, request.nickname());

        if (request.introduction() != null) {
            user.updateIntroduction(request.introduction());
        }
        if (request.profileImageUrl() != null) {
            validateProfileImageUrl(request.profileImageUrl());
            user.updateProfileImageUrl(request.profileImageUrl());
        }
        if (request.careerLevelId() != null) {
            CareerLevel careerLevel = careerLevelRepository.findById(request.careerLevelId())
                    .orElseThrow(() -> new CustomException(ExceptionType.CAREER_LEVEL_NOT_FOUND));
            user.updateCareerLevel(careerLevel);
        }

        if (request.jobIds() != null) {
            if (request.jobIds().isEmpty()) {
                throw new CustomException(ExceptionType.JOB_IDS_EMPTY);
            }
            List<UserJob> jobs = buildUserJobs(user, request.jobIds());
            user.replaceUserJobs(jobs);
        }

        if (request.skills() != null) {
            if (request.skills().isEmpty()) {
                throw new CustomException(ExceptionType.SKILL_IDS_EMPTY);
            }
            List<UserSkill> skills = buildUserSkills(user, request.skills());
            user.replaceUserSkills(skills);
        }

        return UserRes.Me.from(user);
    }

    /**
     * 닉네임 중복 검사
     */
    public UserRes.NicknameCheck checkNickname(String nickname) {
        validateNickname(nickname);
        boolean exists = userRepository.existsByNickname(nickname);
        return new UserRes.NicknameCheck(nickname, exists, !exists);
    }

    /**
     * 모든 유저 검색
     */
    public CursorPage<UserRes.UserSearch> searchUsers(
            String keyword,
            Long jobId,
            Long skillId,
            Long cursorId,
            int size
    ) {
        List<User> users = userRepository.searchUsersByCursor(
                keyword,
                jobId,
                skillId,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = users.size() > size;
        if (hasMore) {
            users = users.subList(0, size);
        }

        List<UserRes.UserSearch> items = users.stream()
                .map(UserRes.UserSearch::from)
                .toList();

        String nextCursor = users.isEmpty() ? null : String.valueOf(users.get(users.size() - 1).getId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new CustomException(ExceptionType.NICKNAME_EMPTY);
        }
        String trimmed = nickname.trim();
        int length = trimmed.length();
        if (length < 2) {
            throw new CustomException(ExceptionType.NICKNAME_TOO_SHORT);
        }
        if (length > 10) {
            throw new CustomException(ExceptionType.NICKNAME_TOO_LONG);
        }
        if (trimmed.contains(" ")) {
            throw new CustomException(ExceptionType.NICKNAME_CONTAINS_WHITESPACE);
        }
        if (!trimmed.matches("^[A-Za-z0-9가-힣]+$")) {
            throw new CustomException(ExceptionType.NICKNAME_INVALID_CHARACTERS);
        }
    }

    private void updateNicknameIfPresent(User user, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        if (nickname.equals(user.getNickname())) {
            return;
        }
        validateNickname(nickname);
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ExceptionType.NICKNAME_DUPLICATE);
        }
        user.updateNickname(nickname);
    }

    private List<UserJob> buildUserJobs(User user, List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return List.of();
        }
        long distinctCount = jobIds.stream().distinct().count();
        if (distinctCount != jobIds.size()) {
            throw new CustomException(ExceptionType.JOB_DUPLICATE);
        }
        List<Job> jobs = jobRepository.findAllById(jobIds);
        if (jobs.size() != jobIds.size()) {
            throw new CustomException(ExceptionType.JOB_NOT_FOUND);
        }
        return jobs.stream()
                .map(job -> UserJob.of(user, job))
                .toList();
    }

    private List<UserSkill> buildUserSkills(User user, List<UserReq.SkillOrder> skillOrders) {
        if (skillOrders.isEmpty()) {
            throw new CustomException(ExceptionType.SKILL_IDS_EMPTY);
        }
        if (skillOrders.stream().anyMatch(order -> order.skillId() == null)) {
            throw new CustomException(ExceptionType.SKILL_IDS_EMPTY);
        }
        if (skillOrders.stream().anyMatch(order -> order.displayOrder() == null)) {
            throw new CustomException(ExceptionType.SKILL_DISPLAY_ORDER_REQUIRED);
        }
        long distinctCount = skillOrders.stream()
                .map(UserReq.SkillOrder::skillId)
                .distinct()
                .count();
        if (distinctCount != skillOrders.size()) {
            throw new CustomException(ExceptionType.SKILL_DUPLICATE);
        }
        List<Long> skillIds = skillOrders.stream()
                .map(UserReq.SkillOrder::skillId)
                .toList();
        List<Skill> skills = skillRepository.findAllById(skillIds);
        if (skills.size() != skillIds.size()) {
            throw new CustomException(ExceptionType.SKILL_NOT_FOUND);
        }
        return skillOrders.stream()
                .map(order -> {
                    Skill skill = skills.stream()
                            .filter(s -> s.getId().equals(order.skillId()))
                            .findFirst()
                            .orElseThrow(() -> new CustomException(ExceptionType.SKILL_NOT_FOUND));
                    return UserSkill.of(user, skill, order.displayOrder());
                })
                .toList();
    }

    private void validateProfileImageUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            throw new CustomException(ExceptionType.IMAGE_URL_INVALID);
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new CustomException(ExceptionType.IMAGE_URL_INVALID);
        }
    }
}
