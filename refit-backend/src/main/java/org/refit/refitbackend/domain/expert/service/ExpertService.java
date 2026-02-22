package org.refit.refitbackend.domain.expert.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.dto.ExpertReq;
import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.domain.expert.dto.ExpertSearchRow;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.expert.repository.ExpertRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.UserJob;
import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.refit.refitbackend.domain.user.repository.UserJobRepository;
import org.refit.refitbackend.domain.user.repository.UserSkillRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {
    private final ExpertRepository expertRepository;
    private final ExpertProfileRepository expertProfileRepository;
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
        String keywordPattern = toKeywordPattern(keyword);
        if (shouldReturnEmptyOnFirstPage(keywordPattern, cursorId)) {
            return new CursorPage<>(List.of(), null, false);
        }

        List<Long> fetchedIds = fetchExpertIds(keywordPattern, jobId, skillId, careerLevelId, cursorId, size + 1);
        boolean hasMore = fetchedIds.size() > size;
        List<Long> pageIds = hasMore ? fetchedIds.subList(0, size) : fetchedIds;

        List<ExpertSearchRow> expertRows = fetchExpertRows(pageIds);
        Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUserIds(pageIds);
        Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUserIds(pageIds);

        List<ExpertRes.ExpertListItem> items = toExpertListItems(expertRows, jobsMap, skillsMap);
        String nextCursor = items.isEmpty() ? null : String.valueOf(items.get(items.size() - 1).userId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    public ExpertRes.ExpertDetail getExpertDetail(Long userId) {
        User expert = expertRepository.findExpertById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.EXPERT_NOT_FOUND));

        Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUserIds(List.of(expert.getId()));
        Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUserIds(List.of(expert.getId()));

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

    @Transactional
    public void updateEmbedding(ExpertReq.UpdateEmbedding request) {
        if (!expertProfileRepository.existsById(request.userId())) {
            throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
        }
        String embedding = toVectorLiteral(request.embedding());
        int updated = expertProfileRepository.updateEmbedding(request.userId(), embedding);
        if (updated == 0) {
            throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
        }
    }

    private String toVectorLiteral(java.util.List<Double> embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private Map<Long, List<ExpertRes.JobDto>> getJobsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
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

    private Map<Long, List<ExpertRes.SkillDto>> getSkillsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
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

    private boolean shouldReturnEmptyOnFirstPage(String keywordPattern, Long cursorId) {
        return isFirstPage(cursorId)
                && keywordPattern != null
                && !hasAnyKeywordMatch(keywordPattern);
    }

    private boolean isFirstPage(Long cursorId) {
        return cursorId == null;
    }

    private boolean hasAnyKeywordMatch(String keywordPattern) {
        return expertRepository.existsNicknamePrefixMatch(keywordPattern)
                || expertRepository.existsJobNamePrefixMatch(keywordPattern)
                || expertRepository.existsSkillNamePrefixMatch(keywordPattern);
    }

    private List<Long> fetchExpertIds(
            String keywordPattern,
            Long jobId,
            Long skillId,
            Long careerLevelId,
            Long cursorId,
            int limit
    ) {
        if (keywordPattern == null) {
            return expertRepository.searchExpertIdsByCursorNoKeyword(
                    jobId, skillId, careerLevelId, cursorId, limit
            );
        }
        return expertRepository.searchExpertIdsByCursorWithKeyword(
                keywordPattern, jobId, skillId, careerLevelId, cursorId, limit
        );
    }

    private List<ExpertSearchRow> fetchExpertRows(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return expertRepository.findExpertRowsByUserIds(userIds);
    }

    private List<ExpertRes.ExpertListItem> toExpertListItems(
            List<ExpertSearchRow> expertRows,
            Map<Long, List<ExpertRes.JobDto>> jobsMap,
            Map<Long, List<ExpertRes.SkillDto>> skillsMap
    ) {
        return expertRows.stream()
                .map(expert -> new ExpertRes.ExpertListItem(
                        expert.userId(),
                        expert.nickname(),
                        expert.profileImageUrl(),
                        expert.introduction(),
                        new ExpertRes.CareerLevelDto(expert.careerLevelId(), expert.careerLevelName()),
                        expert.companyName(),
                        Boolean.TRUE.equals(expert.verified()),
                        expert.ratingAvg() != null ? expert.ratingAvg() : 0.0,
                        expert.ratingCount() != null ? expert.ratingCount() : 0,
                        jobsMap.getOrDefault(expert.userId(), List.of()),
                        skillsMap.getOrDefault(expert.userId(), List.of()),
                        expert.lastActiveAt()
                ))
                .toList();
    }

    private String toKeywordPattern(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String escaped = escapeLikeKeyword(trimmed.toLowerCase(Locale.ROOT));
        return escaped + "%";
    }

    private String escapeLikeKeyword(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

}
