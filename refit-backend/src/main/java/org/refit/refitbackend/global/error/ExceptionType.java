package org.refit.refitbackend.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionType {

    /* =======================
     * Common
     * ======================= */
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "invalid cursor"),

    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "unauthorized"),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN", "invalid_token"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "token_expired"),

    SIGNUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "SIGNUP_ALREADY_EXISTS", "user already exists"),
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "EMAIL_DUPLICATE", "email already exists"),
    OAUTH_DUPLICATE(HttpStatus.CONFLICT, "OAUTH_DUPLICATE", "oauth account already exists"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "forbidden"),


    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "service_unavailable"),

    /* =======================
     * Upload
     * ======================= */
    FILE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "FILE_TYPE_INVALID", "invalid file type"),
    CONTENT_TYPE_INVALID(HttpStatus.BAD_REQUEST, "CONTENT_TYPE_INVALID", "invalid content type"),
    FILE_NAME_EMPTY(HttpStatus.BAD_REQUEST, "FILE_NAME_EMPTY", "file name is required"),

    /* =======================
     * Signup / Auth
     * ======================= */
    SIGNUP_OAUTH_PROVIDER_INVALID(HttpStatus.BAD_REQUEST, "SIGNUP_OAUTH_PROVIDER_INVALID", "invalid oauth provider"),
    SIGNUP_OAUTH_ID_EMPTY(HttpStatus.BAD_REQUEST, "SIGNUP_OAUTH_ID_EMPTY", "oauth id is required"),
    SIGNUP_EMAIL_INVALID(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_INVALID", "invalid email format"),
    SIGNUP_USER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "SIGNUP_USER_TYPE_INVALID", "invalid user type"),

    AUTH_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_INVALID_REQUEST", "auth_invalid_request"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "auth_invalid_credentials"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "auth_forbidden"),

    /* =======================
     * User
     * ======================= */
    USER_ID_INVALID(HttpStatus.BAD_REQUEST, "USER_ID_INVALID", "user_id_invalid"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user_not_found"),

    NICKNAME_EMPTY(HttpStatus.BAD_REQUEST, "NICKNAME_EMPTY", "nickname is empty"),
    NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_SHORT", "nickname is too short"),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_LONG", "nickname is too long"),
    NICKNAME_INVALID_CHARACTERS(HttpStatus.BAD_REQUEST, "NICKNAME_INVALID_CHARACTERS", "nickname contains invalid characters"),
    NICKNAME_CONTAINS_WHITESPACE(HttpStatus.BAD_REQUEST, "NICKNAME_CONTAINS_WHITESPACE", "nickname contains whitespace"),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "NICKNAME_DUPLICATE", "nickname already exists"),

    IMAGE_URL_INVALID(HttpStatus.BAD_REQUEST, "IMAGE_URL_INVALID", "invalid image url format"),
    IMAGE_URL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "IMAGE_URL_NOT_ALLOWED", "image url domain not allowed"),

    /* =======================
     * Job / Skill / Career
     * ======================= */
    JOB_NOT_FOUND(HttpStatus.BAD_REQUEST, "JOB_NOT_FOUND", "job not found"),
    JOB_IDS_EMPTY(HttpStatus.BAD_REQUEST, "JOB_IDS_EMPTY", "job_ids is required"),
    JOB_DUPLICATE(HttpStatus.BAD_REQUEST, "JOB_DUPLICATE", "duplicate job in request"),

    SKILL_NOT_FOUND(HttpStatus.BAD_REQUEST, "SKILL_NOT_FOUND", "skill not found"),
    SKILL_DUPLICATE(HttpStatus.BAD_REQUEST, "SKILL_DUPLICATE", "duplicate skill in request"),
    SKILL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "SKILL_LIMIT_EXCEEDED", "skill count exceeds maximum"),

    CAREER_LEVEL_NOT_FOUND(HttpStatus.BAD_REQUEST, "CAREER_LEVEL_NOT_FOUND", "career level not found"),

    /* =======================
     * Expert
     * ======================= */
    EXPERT_USER_ID_INVALID(HttpStatus.BAD_REQUEST, "EXPERT_USER_ID_INVALID", "invalid user id format"),
    EXPERT_FILTER_INVALID(HttpStatus.BAD_REQUEST, "EXPERT_FILTER_INVALID", "invalid filter parameter"),
    EXPERT_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPERT_NOT_FOUND", "expert_not_found"),

    /* =======================
     * Resume
     * ======================= */
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND", "resume_not_found"),
    RESUME_TITLE_EMPTY(HttpStatus.BAD_REQUEST, "RESUME_TITLE_EMPTY", "resume title is required"),
    RESUME_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "RESUME_TITLE_TOO_LONG", "resume title is too long"),
    RESUME_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "RESUME_LIMIT_EXCEEDED", "resume count exceeds maximum"),

    RESUME_FILE_NOT_PDF(HttpStatus.BAD_REQUEST, "RESUME_FILE_NOT_PDF", "file format must be pdf"),
    RESUME_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "RESUME_FILE_TOO_LARGE", "file size exceeds maximum"),
    RESUME_FILE_URL_INVALID(HttpStatus.BAD_REQUEST, "RESUME_FILE_URL_INVALID", "invalid file url"),
    RESUME_MODE_INVALID(HttpStatus.BAD_REQUEST, "RESUME_MODE_INVALID", "parsing mode must be sync or async"),

    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "task_not_found"),

    /* =======================
     * Chat
     * ======================= */
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND", "chat_not_found"),
    CHAT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "CHAT_ALREADY_CLOSED", "cannot send message to closed chat"),
    MESSAGE_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "MESSAGE_CONTENT_EMPTY", "message content is required"),
    MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "MESSAGE_CONTENT_TOO_LONG", "message content is too long"),

    CHAT_STATUS_INVALID(HttpStatus.BAD_REQUEST, "CHAT_STATUS_INVALID", "status must be ACCEPTED or REJECTED"),
    CHAT_ALREADY_RESPONDED(HttpStatus.BAD_REQUEST, "CHAT_ALREADY_RESPONDED", "chat request already responded"),

    /* =======================
     * Review / Feedback
     * ======================= */
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW_ALREADY_EXISTS", "review already submitted"),
    REVIEW_RATING_INVALID(HttpStatus.BAD_REQUEST, "REVIEW_RATING_INVALID", "rating must be between 1 and 5"),
    REVIEW_COMMENT_TOO_LONG(HttpStatus.BAD_REQUEST, "REVIEW_COMMENT_TOO_LONG", "comment is too long"),
    CHAT_NOT_CLOSED(HttpStatus.BAD_REQUEST, "CHAT_NOT_CLOSED", "can only review after chat is closed"),

    FEEDBACK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FEEDBACK_ALREADY_EXISTS", "feedback already submitted"),
    FEEDBACK_ANSWER_INVALID(HttpStatus.BAD_REQUEST, "FEEDBACK_ANSWER_INVALID", "invalid answer for question"),
    FEEDBACK_ANSWER_MISSING(HttpStatus.BAD_REQUEST, "FEEDBACK_ANSWER_MISSING", "required question not answered"),

    /* =======================
     * Notification
     * ======================= */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "notification_not_found"),
    NOTIFICATION_ALREADY_READ(HttpStatus.CONFLICT, "NOTIFICATION_ALREADY_READ", "notification already marked as read"),
    NOTIFICATION_IS_READ_INVALID(HttpStatus.BAD_REQUEST, "NOTIFICATION_IS_READ_INVALID", "is_read must be true or false"),

    /* =======================
     * Report
     * ======================= */
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "report_not_found"),
    REPORT_ALREADY_PROCESSING(HttpStatus.CONFLICT, "REPORT_ALREADY_PROCESSING", "report generation already in progress"),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "REPORT_ALREADY_EXISTS", "report already exists for this chat"),
    REPORT_STATUS_NOT_FAILED(HttpStatus.BAD_REQUEST, "REPORT_STATUS_NOT_FAILED", "can only retry failed reports"),
    REPORT_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT_DELETE_NOT_ALLOWED", "cannot delete report in current status"),

    REPORT_IDS_EMPTY(HttpStatus.BAD_REQUEST, "REPORT_IDS_EMPTY", "report_ids is required"),
    REPORT_IDS_TOO_MANY(HttpStatus.BAD_REQUEST, "REPORT_IDS_TOO_MANY", "too many report ids"),

    /* =======================
     * AI Chat
     * ======================= */
    AI_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_CHAT_NOT_FOUND", "ai chat not found"),
    AI_CHAT_PURPOSE_REQUIRED(HttpStatus.BAD_REQUEST, "AI_CHAT_PURPOSE_REQUIRED", "purpose is required"),
    AI_CHAT_PURPOSE_INVALID(HttpStatus.BAD_REQUEST, "AI_CHAT_PURPOSE_INVALID", "invalid purpose"),
    AI_CHAT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "AI_CHAT_ALREADY_CLOSED", "cannot send message to closed ai chat"),
    AI_CHAT_STATUS_INVALID(HttpStatus.BAD_REQUEST, "AI_CHAT_STATUS_INVALID", "status must be CLOSED"),





    INVALID_JSON(HttpStatus.BAD_REQUEST,"INVALID_JSON_REQUEST", "invalid json request"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "server_error"),
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "ai_server_error");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
