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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // @RequestBody @Valid 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        if (error == null) {
            return ResponseUtil.error(ExceptionType.INVALID_REQUEST);
        }

        return ResponseUtil.error(mapValidationError(error));
    }

    // @RequestParam @Validated 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        ConstraintViolation<?> violation = ex.getConstraintViolations().stream()
                .findFirst()
                .orElse(null);

        if (violation == null) {
            return ResponseUtil.error(ExceptionType.INVALID_REQUEST);
        }

        String message = violation.getMessage();
        String field = null;
        if (violation.getPropertyPath() != null) {
            String path = violation.getPropertyPath().toString();
            int lastDot = path.lastIndexOf('.');
            field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        }
        String constraint = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();

        return ResponseUtil.error(mapValidationMessage(message, field, constraint, violation.getInvalidValue()));
    }

    // JSON 파싱 실패 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseUtil.error(ExceptionType.INVALID_JSON);
    }

    // DB 무결성 제약 위반 (ex: UNIQUE, FK 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("⚠Data integrity violation: {}", ex.getMessage());

        // 예외 메시지 기반으로 세분화 가능
        // 그 외 제약 조건 위반 (ex: FK 에러 등)
        return ResponseUtil.error(ExceptionType.INTERNAL_SERVER_ERROR);
    }

    // 커스텀 비즈니스 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustom(CustomException ex) {
        if (ex.getData() != null) {
            return ResponseUtil.error(ex.getExceptionType(), ex.getData());
        }
        return ResponseUtil.error(ex.getExceptionType());
    }

    // 그 외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAny(Exception ex) {
        log.error("Unexpected", ex);
        return ResponseUtil.error(ExceptionType.INTERNAL_SERVER_ERROR);
    }


    private ExceptionType mapValidationError(FieldError error) {
        return mapValidationMessage(
                error.getDefaultMessage(),
                error.getField(),
                error.getCode(),
                error.getRejectedValue()
        );
    }

    private ExceptionType mapValidationMessage(String message, String field, String constraint, Object rejected) {
        if (message == null) {
            message = "validation_error";
        }

        ExceptionType byMessage = resolveByMessage(message);
        if (byMessage != null) {
            return byMessage;
        }

        return switch (message) {

            /* =======================
             * Signup / Auth
             * ======================= */

            // OAuth
            case "signup_oauth_provider_invalid" ->
                    ExceptionType.SIGNUP_OAUTH_PROVIDER_INVALID;

            case "signup_oauth_id_empty" ->
                    ExceptionType.SIGNUP_OAUTH_ID_EMPTY;

            case "auth_code_required" ->
                    ExceptionType.AUTH_CODE_REQUIRED;

            case "refresh_token_required" ->
                    ExceptionType.REFRESH_TOKEN_REQUIRED;

            case "user_id_required" ->
                    ExceptionType.USER_ID_REQUIRED;

            // Email
            case "signup_email_empty" ->
                    ExceptionType.SIGNUP_EMAIL_INVALID; // empty도 invalid로 통일

            case "signup_email_invalid" ->
                    ExceptionType.SIGNUP_EMAIL_INVALID;

            // User Type
            case "signup_user_type_invalid" ->
                    ExceptionType.SIGNUP_USER_TYPE_INVALID;

            case "nickname_empty" ->
                    ExceptionType.NICKNAME_EMPTY;

            case "career_level_not_found" ->
                    ExceptionType.CAREER_LEVEL_NOT_FOUND;

            /* =======================
             * Resume
             * ======================= */
            case "resume_title_empty" ->
                    ExceptionType.RESUME_TITLE_EMPTY;
            case "resume_title_too_long" ->
                    ExceptionType.RESUME_TITLE_TOO_LONG;
            case "resume_is_fresher_invalid" ->
                    ExceptionType.RESUME_IS_FRESHER_INVALID;
            case "resume_education_level_invalid" ->
                    ExceptionType.RESUME_EDUCATION_LEVEL_INVALID;
            case "resume_content_invalid" ->
                    ExceptionType.RESUME_CONTENT_INVALID;
            case "resume_id_invalid" ->
                    ExceptionType.RESUME_ID_INVALID;

            /* =======================
             * Chat
             * ======================= */

            case "message_content_required" ->
                    ExceptionType.MESSAGE_CONTENT_EMPTY;

            case "message_content_length_invalid" ->
                    ExceptionType.MESSAGE_CONTENT_TOO_LONG;

            case "chat_receiver_required" ->
                    ExceptionType.CHAT_RECEIVER_REQUIRED;

            case "chat_request_type_required" ->
                    ExceptionType.CHAT_REQUEST_TYPE_REQUIRED;

            case "chat_room_id_required", "chat_id_required" ->
                    ExceptionType.CHAT_ID_REQUIRED;

            case "chat_status_required" ->
                    ExceptionType.CHAT_STATUS_REQUIRED;

            case "message_id_required" ->
                    ExceptionType.MESSAGE_ID_REQUIRED;

            case "chat_job_post_url_too_long" ->
                    ExceptionType.CHAT_JOB_POST_URL_TOO_LONG;

            /* =======================
             * Expert
             * ======================= */
            case "expert_user_id_invalid" ->
                    ExceptionType.EXPERT_USER_ID_INVALID;

            case "embedding_empty" ->
                    ExceptionType.EXPERT_EMBEDDING_EMPTY;

            case "user_type_invalid" ->
                    ExceptionType.USER_TYPE_INVALID;

            /* =======================
             * Email Verification
             * ======================= */
            case "email_required", "email_invalid" ->
                    ExceptionType.EMAIL_FORMAT_INVALID;

            case "verification_code_required" ->
                    ExceptionType.VERIFICATION_CODE_INVALID;

            /* =======================
             * Fallback
             * ======================= */
            default -> resolveByFieldAndConstraint(field, constraint, rejected);
        };
    }

    private ExceptionType resolveByMessage(String message) {
        for (ExceptionType type : ExceptionType.values()) {
            if (type.getMessage().equals(message)) {
                return type;
            }
        }
        return null;
    }

    private ExceptionType resolveByFieldAndConstraint(String field, String constraint, Object rejected) {
        if ("nickname".equals(field)) {
            if ("NotBlank".equals(constraint)) {
                return ExceptionType.NICKNAME_EMPTY;
            }
            if ("Size".equals(constraint) && rejected instanceof String nickname) {
                int length = nickname.length();
                if (length < 2) {
                    return ExceptionType.NICKNAME_TOO_SHORT;
                }
                if (length > 10) {
                    return ExceptionType.NICKNAME_TOO_LONG;
                }
            }
        }

        return ExceptionType.INVALID_REQUEST;
    }


}
