package org.refit.refitbackend.domain.expert.dto;

import java.time.LocalDateTime;

public record ExpertSearchRow(
        Long userId,
        String nickname,
        String profileImageUrl,
        String introduction,
        Long careerLevelId,
        String careerLevelName,
        String companyName,
        Boolean verified,
        Double ratingAvg,
        Integer ratingCount,
        LocalDateTime lastActiveAt
) {
}
