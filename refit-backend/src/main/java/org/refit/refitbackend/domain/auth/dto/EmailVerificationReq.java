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
            @NotBlank(message = "이메일 형식이 올바르지 않습니다.")
            String email
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "이메일 인증 코드 확인 요청")
    public record Verify(
            @Schema(description = "회사 이메일", example = "user@navercorp.com")
            @NotBlank(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @Schema(description = "인증 코드 (6자리)", example = "123456")
            @NotBlank(message = "인증 코드가 올바르지 않습니다.")
            @Size(min = 6, max = 6, message = "인증 코드가 올바르지 않습니다.")
            String code
    ) {}
}
