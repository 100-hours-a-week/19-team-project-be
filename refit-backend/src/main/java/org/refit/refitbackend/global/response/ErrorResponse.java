package org.refit.refitbackend.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "응답 코드", example = "DOMAIN_ERROR")
        String code,
        @Schema(description = "응답 메시지", example = "error_description")
        String message,
        @Schema(description = "응답 데이터", example = "null")
        Object data
) {}
