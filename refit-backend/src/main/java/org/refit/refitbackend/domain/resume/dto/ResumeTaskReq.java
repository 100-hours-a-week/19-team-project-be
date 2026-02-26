package org.refit.refitbackend.domain.resume.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class ResumeTaskReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateTask(
            @Schema(
                    description = "파싱할 이력서 PDF 파일 URL (비동기 작업 생성용)",
                    example = "https://refit-storage-dev.s3.ap-northeast-2.amazonaws.com/resumes/original/example.pdf"
            )
            @NotBlank(message = "이력서 파일 URL이 올바르지 않습니다.")
            String fileUrl
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Parse(
            @Schema(
                    description = "파싱할 이력서 PDF 파일 URL (V1 동기 파싱용)",
                    example = "https://refit-storage-dev.s3.ap-northeast-2.amazonaws.com/resumes/original/example.pdf"
            )
            @NotBlank(message = "이력서 파일 URL이 올바르지 않습니다.")
            String fileUrl,
            @Schema(
                    description = "파싱 모드 (V1은 sync만 지원, async는 지원하지 않음)",
                    example = "sync"
            )
            @NotBlank(message = "파싱 모드는 sync 또는 async여야 합니다.")
            String mode
    ) {}
}
