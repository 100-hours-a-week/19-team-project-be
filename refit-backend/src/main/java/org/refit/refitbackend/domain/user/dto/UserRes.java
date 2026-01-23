package org.refit.refitbackend.domain.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "현직자 검색 응답")
    public record ExpertSearch(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "홍길동")
            String nickname,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl,

            @Schema(description = "소개")
            String introduction,

            @Schema(description = "회사명", example = "네이버")
            String companyName,

            @Schema(description = "경력 레벨", example = "5년")
            String careerLevel,

            @Schema(description = "평균 평점", example = "4.5")
            Double ratingAvg,

            @Schema(description = "리뷰 개수", example = "10")
            Integer ratingCount,

            @Schema(description = "직무 목록")
            List<String> jobs,

            @Schema(description = "스킬 목록")
            List<String> skills
    ) {
        public static ExpertSearch from(User user) {
            // ExpertProfile이 없으면 기본값 사용
            String companyName = null;
            Double ratingAvg = 0.0;
            Integer ratingCount = 0;

            // UserJob에서 직무 추출
            List<String> jobs = user.getUserJobs().stream()
                    .map(uj -> uj.getJob().getName())
                    .toList();

            // UserSkill에서 스킬 추출
            List<String> skills = user.getUserSkills().stream()
                    .map(us -> us.getSkill().getName())
                    .toList();

            return new ExpertSearch(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getIntroduction(),
                    companyName,
                    user.getCareerLevel().getLevel(),
                    ratingAvg,
                    ratingCount,
                    jobs,
                    skills
            );
        }
    }

    public record ExpertCursorResponse(
            List<UserRes.ExpertSearch> experts,
            String nextCursor,
            boolean hasMore
    ) {}

    @Schema(description = "유저 검색 응답")
    public record UserSearch(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "홍길동")
            String nickname,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl,

            @Schema(description = "소개")
            String introduction,

            @Schema(description = "사용자 타입", example = "JOB_SEEKER")
            String userType,

            @Schema(description = "경력 레벨", example = "5년")
            String careerLevel,

            @Schema(description = "직무 목록")
            List<String> jobs,

            @Schema(description = "스킬 목록")
            List<String> skills
    ) {
        public static UserSearch from(User user) {
            List<String> jobs = user.getUserJobs().stream()
                    .map(uj -> uj.getJob().getName())
                    .toList();
            List<String> skills = user.getUserSkills().stream()
                    .map(us -> us.getSkill().getName())
                    .toList();
            return new UserSearch(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getIntroduction(),
                    user.getUserType().name(),
                    user.getCareerLevel().getLevel(),
                    jobs,
                    skills
            );
        }
    }

    public record UserCursorResponse(
            List<UserRes.UserSearch> users,
            String nextCursor,
            boolean hasMore
    ) {}

    @Schema(description = "닉네임 중복 검사 응답")
    public record NicknameCheck(
            @Schema(description = "닉네임", example = "테스터")
            String nickname,

            @Schema(description = "중복 여부", example = "false")
            boolean exists,

            @Schema(description = "사용 가능 여부", example = "true")
            boolean available
    ) {}
}
