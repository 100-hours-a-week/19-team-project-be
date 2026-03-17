package org.refit.refitbackend.domain.expertfeedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ExpertFeedbackRes {

    public record CreatedId(
            @Schema(description = "생성된 피드백 ID", example = "21")
            Long id
    ) {
    }

    public record BatchInsertResult(
            @Schema(description = "삽입된 건수", example = "20", name = "inserted_count")
            int insertedCount
    ) {
    }
}
