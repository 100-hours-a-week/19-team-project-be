package org.refit.refitbackend.domain.expert.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class ExpertReq {

    public record UpdateEmbedding(
            @NotNull(message = "expert_user_id_invalid")
            @Positive(message = "expert_user_id_invalid")
            Long userId,

            @NotEmpty(message = "embedding_empty")
            List<Double> embedding
    ) {}
}
