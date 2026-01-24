package org.refit.refitbackend.domain.expert.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.UserJob;
import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.refit.refitbackend.domain.user.repository.UserJobRepository;
import org.refit.refitbackend.domain.user.repository.UserSkillRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {

    private final ExpertRepository expertRepository;
    private final UserJobRepository userJobRepository;
    private final UserSkillRepository userSkillRepository;

    public CursorPage<ExpertRes.ExpertListItem> searchExperts(
            String keyword,
            Long jobId,
            Long skillId,
            Long careerLevelId,
            Long cursorId,
            int size
    ) {
        List<User> experts = expertRepository.searchExpertsByCursor(
                keyword,
                jobId,
                skillId,
                careerLevelId,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = experts.size() > size;
        if (hasMore) {
            experts = experts.subList(0, size);
        }

        Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUserIds(experts);
        Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUserIds(experts);

        List<ExpertRes.ExpertListItem> items = experts.stream()
                .map(user -> {
                    ExpertProfile profile = user.getExpertProfile();
                    return ExpertRes.ExpertListItem.from(
                            user,
                            jobsMap.getOrDefault(user.getId(), List.of()),
                            skillsMap.getOrDefault(user.getId(), List.of()),
                            profile != null ? profile.getCompanyName() : null,
                            profile != null && profile.isVerified(),
                            profile != null ? profile.getRatingAvg() : 0.0,
                            profile != null ? profile.getRatingCount() : 0,
                            profile != null ? profile.getLastActiveAt() : null
                    );
                })
                .toList();

        String nextCursor = experts.isEmpty() ? null : String.valueOf(experts.get(experts.size() - 1).getId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    public ExpertRes.ExpertDetail getExpertDetail(Long userId) {
        User expert = expertRepository.findExpertById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.EXPERT_NOT_FOUND));

        Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUserIds(List.of(expert));
        Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUserIds(List.of(expert));

        ExpertProfile profile = expert.getExpertProfile();

        return ExpertRes.ExpertDetail.from(
                expert,
                jobsMap.getOrDefault(expert.getId(), List.of()),
                skillsMap.getOrDefault(expert.getId(), List.of()),
                profile != null ? profile.getCompanyName() : null,
                profile != null && profile.isVerified(),
                profile != null ? profile.getVerifiedAt() : null,
                profile != null ? profile.getRatingAvg() : 0.0,
                profile != null ? profile.getRatingCount() : 0,
                profile != null ? profile.getLastActiveAt() : null
        );
    }

    private Map<Long, List<ExpertRes.JobDto>> getJobsByUserIds(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        List<UserJob> userJobs = userJobRepository.findAllByUserIdIn(userIds);

        return userJobs.stream()
                .collect(Collectors.groupingBy(
                        uj -> uj.getUser().getId(),
                        Collectors.mapping(
                                uj -> new ExpertRes.JobDto(uj.getJob().getId(), uj.getJob().getName()),
                                Collectors.toList()
                        )
                ));
    }

    private Map<Long, List<ExpertRes.SkillDto>> getSkillsByUserIds(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        List<UserSkill> userSkills = userSkillRepository.findAllByUserIdIn(userIds);

        return userSkills.stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .collect(Collectors.groupingBy(
                        us -> us.getUser().getId(),
                        Collectors.mapping(
                                us -> new ExpertRes.SkillDto(us.getSkill().getId(), us.getSkill().getName()),
                                Collectors.toList()
                        )
                ));
    }
}
