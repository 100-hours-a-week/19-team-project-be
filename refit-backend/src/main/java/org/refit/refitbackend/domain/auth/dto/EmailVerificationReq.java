package org.refit.refitbackend.domain.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailVerificationReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "이메일 인증 코드 발송 요청")
    public record Send(
            @Schema(description = "회사 이메일", example = "user@navercorp.com")
            @NotBlank(message = "email_required")
            String email
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "이메일 인증 코드 확인 요청")
    public record Verify(
            @Schema(description = "회사 이메일", example = "user@navercorp.com")
            @NotBlank(message = "email_required")
            String email,

            @Schema(description = "인증 코드 (6자리)", example = "123456")
            @NotBlank(message = "verification_code_required")
            @Size(min = 6, max = 6, message = "verification_code_required")
            String code
    ) {}
}
