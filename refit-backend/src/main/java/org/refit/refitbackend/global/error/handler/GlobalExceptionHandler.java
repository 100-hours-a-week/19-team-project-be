package org.refit.refitbackend.global.error.handler;


import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // @RequestBody @Valid 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .orElse("validation_error");

        ExceptionType type = mapValidationError(message);

        return ResponseUtil.error(type);
    }

    // JSON 파싱 실패 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseUtil.error(ExceptionType.INVALID_JSON);
    }

    // DB 무결성 제약 위반 (ex: UNIQUE, FK 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("⚠Data integrity violation: {}", ex.getMessage());

        // 예외 메시지 기반으로 세분화 가능
        // 그 외 제약 조건 위반 (ex: FK 에러 등)
        return ResponseUtil.error(ExceptionType.INTERNAL_SERVER_ERROR);
    }

    // 커스텀 비즈니스 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException ex) {
        return ResponseUtil.error(ex.getExceptionType());
    }

    // 그 외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        log.error("Unexpected", ex);
        return ResponseUtil.error(ExceptionType.INTERNAL_SERVER_ERROR);
    }


    private ExceptionType mapValidationError(String code) {
        return switch (code) {

            /* =======================
             * Signup / Auth
             * ======================= */

            // OAuth
            case "signup_oauth_provider_invalid" ->
                    ExceptionType.SIGNUP_OAUTH_PROVIDER_INVALID;

            case "signup_oauth_id_empty" ->
                    ExceptionType.SIGNUP_OAUTH_ID_EMPTY;

            // Email
            case "signup_email_empty" ->
                    ExceptionType.SIGNUP_EMAIL_INVALID; // empty도 invalid로 통일

            case "signup_email_invalid" ->
                    ExceptionType.SIGNUP_EMAIL_INVALID;

            // User Type
            case "signup_user_type_invalid" ->
                    ExceptionType.SIGNUP_USER_TYPE_INVALID;

            /* =======================
             * Fallback
             * ======================= */
            default -> ExceptionType.AUTH_INVALID_REQUEST;
        };
    }


}
