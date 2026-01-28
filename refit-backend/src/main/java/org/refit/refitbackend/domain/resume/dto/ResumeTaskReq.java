package org.refit.refitbackend.domain.resume.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

public class ResumeTaskReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Parse(
            @NotBlank(message = "이력서 파일 URL이 올바르지 않습니다.")
            String fileUrl,
            @NotBlank(message = "파싱 모드는 sync 또는 async여야 합니다.")
            String mode
    ) {}
}
