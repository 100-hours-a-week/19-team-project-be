package org.refit.refitbackend.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiResponse<T>(
        @Schema(description = "응답 코드", example = "NICKNAME_EMPTY")
        String code,
        @Schema(description = "응답 메시지", example = "nickname is empty")
        String message,
        @Schema(description = "응답 데이터", example = "Any Data")
        T data
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("OK", message, data);
    }
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>("CREATED", message, data);
    }
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
