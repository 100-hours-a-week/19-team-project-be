package org.refit.refitbackend.domain.expert.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import org.refit.refitbackend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class ExpertRes {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CareerLevelDto(
            Long id,
            String level
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record JobDto(
            Long id,
            String name
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SkillDto(
            Long id,
            String name
    ) {}

    @Schema(description = "현직자 목록 아이템")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ExpertListItem(
            Long userId,
            String nickname,
            String profileImageUrl,
            String introduction,
            CareerLevelDto careerLevel,
            String companyName,
            boolean verified,
            Double ratingAvg,
            Integer ratingCount,
            List<JobDto> jobs,
            List<SkillDto> skills,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime lastActiveAt
    ) {
        public static ExpertListItem from(
                User user,
                List<JobDto> jobs,
                List<SkillDto> skills,
                String companyName,
                boolean verified,
                Double ratingAvg,
                Integer ratingCount,
                LocalDateTime lastActiveAt
        ) {
            return new ExpertListItem(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getIntroduction(),
                    new CareerLevelDto(user.getCareerLevel().getId(), user.getCareerLevel().getLevel()),
                    companyName,
                    verified,
                    ratingAvg,
                    ratingCount,
                    jobs,
                    skills,
                    lastActiveAt
            );
        }
    }

    @Schema(description = "현직자 상세")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ExpertDetail(
            Long userId,
            String nickname,
            String profileImageUrl,
            String introduction,
            String companyName,
            boolean verified,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime verifiedAt,
            CareerLevelDto careerLevel,
            List<JobDto> jobs,
            List<SkillDto> skills,
            Double ratingAvg,
            Integer ratingCount,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime lastActiveAt
    ) {
        public static ExpertDetail from(
                User user,
                List<JobDto> jobs,
                List<SkillDto> skills,
                String companyName,
                boolean verified,
                LocalDateTime verifiedAt,
                Double ratingAvg,
                Integer ratingCount,
                LocalDateTime lastActiveAt
        ) {
            return new ExpertDetail(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getIntroduction(),
                    companyName,
                    verified,
                    verifiedAt,
                    new CareerLevelDto(user.getCareerLevel().getId(), user.getCareerLevel().getLevel()),
                    jobs,
                    skills,
                    ratingAvg,
                    ratingCount,
                    lastActiveAt
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ExpertCursorResponse(
            List<ExpertListItem> experts,
            String nextCursor,
            boolean hasMore
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationItem(
            @JsonProperty("user_id")
            Long userId,
            String nickname,
            @JsonProperty("company_name")
            String companyName,
            boolean verified,
            @JsonProperty("rating_avg")
            Double ratingAvg,
            @JsonProperty("rating_count")
            Integer ratingCount,
            @JsonProperty("response_rate")
            Double responseRate,
            List<String> skills,
            List<String> jobs,
            String introduction,
            @JsonProperty("similarity_score")
            Double similarityScore,
            @JsonProperty("filter_type")
            String filterType,
            @JsonProperty("ground_truth")
            String groundTruth,
            @JsonProperty("last_active_at")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime lastActiveAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationResponse(
            @JsonProperty("user_id")
            Long userId,
            List<RecommendationItem> recommendations,
            @JsonProperty("total_count")
            Integer totalCount,
            Object evaluation
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MentorEmbeddingUpdateResponse(
            boolean success,
            @JsonProperty("user_id")
            Long userId,
            String message
    ) {}
}
