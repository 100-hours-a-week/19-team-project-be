package org.refit.refitbackend.global.util;

import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseUtil {
    private ResponseUtil() {throw new AssertionError("Utility class");}

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
        return ResponseEntity.ok(ApiResponse.ok(message, null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(message, data));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(code, message));
    }
    public static <T> ResponseEntity<ApiResponse<T>> error(ExceptionType type) {
        return ResponseEntity.status(type.getStatus())
                .body(ApiResponse.error(type.getCode(), type.getMessage()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(ExceptionType type, T data) {
        return ResponseEntity.status(type.getStatus())
                .body(ApiResponse.errorWithData(type.getCode(), type.getMessage(), data));
    }
}
