package org.refit.refitbackend.domain.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

public class ResumeTaskRes {

    public record ParsedResult(
            @Schema(description = "신입 여부", example = "false")
            boolean isFresher,
            @Schema(description = "학력 수준", example = "대졸")
            String educationLevel,
            @Schema(description = "파싱된 이력서 구조화 JSON")
            JsonNode contentJson,
            @Schema(description = "원문 일부 발췌 텍스트", example = "경력사항 ...")
            String rawTextExcerpt
    ) {}

    public record TaskResult(
            @Schema(description = "비동기 작업 ID", example = "task_resume_1234567890abcdef")
            String taskId,
            @Schema(description = "작업 상태 (PROCESSING / COMPLETED / FAILED)", example = "PROCESSING")
            String status,
            @Schema(description = "파싱 완료 시 결과 (PROCESSING/FAILED 상태에서는 null)")
            ParsedResult result
    ) {}
}
