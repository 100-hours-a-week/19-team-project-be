package org.refit.refitbackend.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserReq {

    public record UpdateMe(
            @Size(min = 2, max = 10)
            String nickname,
            String introduction,
            String profileImageUrl,
            Long careerLevelId,
            List<Long> jobIds,
            List<SkillOrder> skills
    ) {}

    public record SkillOrder(
            @NotNull(message = "스킬 ID가 필요합니다.")
            Long skillId,
            @NotNull(message = "스킬 순서가 필요합니다.")
            Integer displayOrder
    ) {}
}
