package org.refit.refitbackend.domain.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.refit.refitbackend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class UserRes {

    /* =======================
     * 유저 상세 조회
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Detail(
            Long id,
            String email,
            String nickname,
            String userType,
            String careerLevel,
            List<String> jobs,
            List<String> skills,
            String profileImageUrl,
            String introduction,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Detail from(User user) {
            return new Detail(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getUserType().name(),
                    user.getCareerLevel().getLevel(),
                    user.getUserJobs().stream()
                            .map(uj -> uj.getJob().getName())
                            .toList(),
                    user.getUserSkills().stream()
                            .map(us -> us.getSkill().getName())
                            .toList(),
                    user.getProfileImageUrl(),
                    user.getIntroduction(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }

    /* =======================
     * 내 정보 조회
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Me(
            Long id,
            String email,
            String nickname,
            String userType,
            CareerLevelDto careerLevel,
            String introduction,
            String profileImageUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<JobDto> jobs,
            List<SkillDto> skills
    ) {
        public static Me from(User user) {
            return new Me(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getUserType().name(),
                    new CareerLevelDto(
                            user.getCareerLevel().getId(),
                            user.getCareerLevel().getLevel()
                    ),
                    user.getIntroduction(),
                    user.getProfileImageUrl(),
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    user.getUserJobs().stream()
                            .map(uj -> new JobDto(
                                    uj.getJob().getId(),
                                    uj.getJob().getName()
                            ))
                            .toList(),
                    user.getUserSkills().stream()
                            .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                            .map(us -> new SkillDto(
                                    us.getSkill().getId(),
                                    us.getSkill().getName(),
                                    us.getDisplayOrder()
                            ))
                            .toList()
            );
        }
    }

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
            String name,
            Integer displayOrder
    ) {}
}
