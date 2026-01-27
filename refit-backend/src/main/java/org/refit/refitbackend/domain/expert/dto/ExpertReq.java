package org.refit.refitbackend.domain.expert.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class ExpertReq {

    public record UpdateEmbedding(
            @NotNull(message = "유효하지 않은 현직자 ID입니다.")
            @Positive(message = "유효하지 않은 현직자 ID입니다.")
            Long userId,

            @NotEmpty(message = "임베딩 벡터가 비어 있습니다.")
            List<Double> embedding
    ) {}
}
