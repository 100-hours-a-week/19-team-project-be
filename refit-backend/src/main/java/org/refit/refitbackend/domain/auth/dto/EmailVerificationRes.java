package org.refit.refitbackend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class EmailVerificationRes {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "이메일 인증 코드 발송 응답")
    public record Send(
            @Schema(description = "이메일", example = "user@navercorp.com")
            String email,

            @Schema(description = "만료 시각")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime expiresAt,

            @Schema(description = "발송 횟수", example = "1")
            int sentCount,

            @Schema(description = "남은 시도 횟수", example = "2")
            int remainingAttempts
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "이메일 인증 성공 응답")
    public record Verify(
            @Schema(description = "이메일", example = "user@navercorp.com")
            String email,

            @Schema(description = "인증 완료 시각")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime verifiedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Schema(description = "이메일 인증 요청 제한 응답")
    public record RateLimit(
            @Schema(description = "재시도까지 남은 초", example = "600")
            long retryAfterSeconds
    ) {}
}
