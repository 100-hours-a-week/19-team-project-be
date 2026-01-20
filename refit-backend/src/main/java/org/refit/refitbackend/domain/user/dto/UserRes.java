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
}
