package org.refit.refitbackend.domain.expertfeedback.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.refit.refitbackend.domain.expertfeedback.entity.ExpertFeedbackJobTag;
import org.refit.refitbackend.domain.expertfeedback.entity.ExpertFeedbackQuestionType;
import org.refit.refitbackend.domain.expertfeedback.entity.ExpertFeedbackSourceType;

import java.util.List;

public class ExpertFeedbackReq {

    public record CreateFeedback(
            @Schema(description = "멘토 ID. 시드 데이터는 0", example = "0")
            @NotNull @PositiveOrZero Long mentorId,

            @Schema(description = "질문", example = "RAG 프로젝트 경험은 있는데...")
            @NotBlank String question,

            @Schema(description = "답변", example = "RAG 시스템의 성능을 어떻게 평가했는지...")
            @NotBlank String answer,

            @Schema(description = "직무 태그", example = "AI")
            @NotNull ExpertFeedbackJobTag jobTag,

            @Schema(description = "질문 유형", example = "RESUME_FEEDBACK")
            @NotNull ExpertFeedbackQuestionType questionType,

            @Schema(description = "임베딩 생성용 텍스트", example = "RAG 운영 경험 부족 보완...")
            String embeddingText,

            @Schema(description = "데이터 출처", example = "SEED")
            @NotNull ExpertFeedbackSourceType sourceType,

            @Schema(description = "품질 점수", example = "5", minimum = "1", maximum = "5")
            @NotNull @Min(1) @Max(5) Integer qualityScore,

            @ArraySchema(schema = @Schema(type = "number", format = "float"))
            @Size(min = 1024, max = 1024) List<Float> embedding
    ) {
    }

    public record BatchCreateFeedback(
            @Schema(description = "피드백 목록")
            @NotEmpty List<@Valid CreateFeedback> feedbacks
    ) {
    }
}
