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
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "잘못된 커서입니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "토큰이 만료되었습니다."),

    // ExceptionType enum에 추가
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "auth_invalid_code", "유효하지 않은 인증 코드입니다."),

    SIGNUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "SIGNUP_ALREADY_EXISTS", "이미 가입된 사용자입니다."),
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "EMAIL_DUPLICATE", "이미 사용 중인 이메일입니다."),
    OAUTH_DUPLICATE(HttpStatus.CONFLICT, "OAUTH_DUPLICATE", "이미 연동된 OAuth 계정입니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    STORAGE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "STORAGE_ACCESS_FORBIDDEN", "해당 파일에 접근 권한이 없습니다."),


    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."),

    /* =======================
     * Upload
     * ======================= */
    FILE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "FILE_TYPE_INVALID", "지원하지 않는 파일 형식입니다."),
    CONTENT_TYPE_INVALID(HttpStatus.BAD_REQUEST, "CONTENT_TYPE_INVALID", "지원하지 않는 콘텐츠 타입입니다."),
    FILE_NAME_EMPTY(HttpStatus.BAD_REQUEST, "FILE_NAME_EMPTY", "파일명이 필요합니다."),
    FILE_SIZE_REQUIRED(HttpStatus.BAD_REQUEST, "FILE_SIZE_REQUIRED", "파일 크기가 필요합니다."),
    FILE_SIZE_INVALID(HttpStatus.BAD_REQUEST, "FILE_SIZE_INVALID", "파일 크기가 올바르지 않습니다."),

    /* =======================
     * Signup / Auth
     * ======================= */
    SIGNUP_OAUTH_PROVIDER_INVALID(HttpStatus.BAD_REQUEST, "SIGNUP_OAUTH_PROVIDER_INVALID", "유효하지 않은 OAuth 제공자입니다."),
    SIGNUP_OAUTH_ID_EMPTY(HttpStatus.BAD_REQUEST, "SIGNUP_OAUTH_ID_EMPTY", "OAuth ID가 필요합니다."),
    SIGNUP_EMAIL_INVALID(HttpStatus.BAD_REQUEST, "SIGNUP_EMAIL_INVALID", "회원가입 이메일 형식이 올바르지 않습니다."),
    SIGNUP_USER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "SIGNUP_USER_TYPE_INVALID", "회원가입 사용자 유형이 올바르지 않습니다."),

    AUTH_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_INVALID_REQUEST", "인증 요청이 올바르지 않습니다."),
    AUTH_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_CODE_REQUIRED", "인증 코드가 필요합니다."),
    REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "REFRESH_TOKEN_REQUIRED", "리프레시 토큰이 필요합니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "인증 정보가 올바르지 않습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "인증 권한이 없습니다."),

    /* =======================
     * User
     * ======================= */
    USER_ID_INVALID(HttpStatus.BAD_REQUEST, "USER_ID_INVALID", "유효하지 않은 사용자 ID입니다."),
    USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED", "사용자 ID가 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    USER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "USER_TYPE_INVALID", "유효하지 않은 사용자 유형입니다."),

    NICKNAME_EMPTY(HttpStatus.BAD_REQUEST, "NICKNAME_EMPTY", "닉네임을 입력해 주세요."),
    NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_SHORT", "닉네임이 너무 짧습니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_LONG", "닉네임이 너무 깁니다."),
    NICKNAME_INVALID_CHARACTERS(HttpStatus.BAD_REQUEST, "NICKNAME_INVALID_CHARACTERS", "닉네임에 사용할 수 없는 문자가 포함되어 있습니다."),
    NICKNAME_CONTAINS_WHITESPACE(HttpStatus.BAD_REQUEST, "NICKNAME_CONTAINS_WHITESPACE", "닉네임에 공백이 포함되어 있습니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "NICKNAME_DUPLICATE", "이미 사용 중인 닉네임입니다."),

    IMAGE_URL_INVALID(HttpStatus.BAD_REQUEST, "IMAGE_URL_INVALID", "이미지 URL 형식이 올바르지 않습니다."),
    IMAGE_URL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "IMAGE_URL_NOT_ALLOWED", "허용되지 않은 이미지 URL 도메인입니다."),
    PROFILE_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "PROFILE_IMAGE_TOO_LARGE", "프로필 이미지 용량이 너무 큽니다."),

    /* =======================
     * Job / Skill / Career
     * ======================= */
    JOB_NOT_FOUND(HttpStatus.BAD_REQUEST, "JOB_NOT_FOUND", "직무를 찾을 수 없습니다."),
    JOB_IDS_EMPTY(HttpStatus.BAD_REQUEST, "JOB_IDS_EMPTY", "직무 ID가 필요합니다."),
    JOB_DUPLICATE(HttpStatus.BAD_REQUEST, "JOB_DUPLICATE", "중복된 직무가 포함되어 있습니다."),

    SKILL_NOT_FOUND(HttpStatus.BAD_REQUEST, "SKILL_NOT_FOUND", "스킬을 찾을 수 없습니다."),
    SKILL_DUPLICATE(HttpStatus.BAD_REQUEST, "SKILL_DUPLICATE", "중복된 스킬이 포함되어 있습니다."),
    SKILL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "SKILL_LIMIT_EXCEEDED", "스킬 개수가 제한을 초과했습니다."),
    SKILL_IDS_EMPTY(HttpStatus.BAD_REQUEST, "SKILL_IDS_EMPTY", "스킬 ID가 필요합니다."),
    SKILL_DISPLAY_ORDER_REQUIRED(HttpStatus.BAD_REQUEST, "SKILL_DISPLAY_ORDER_REQUIRED", "스킬 순서가 필요합니다."),

    CAREER_LEVEL_NOT_FOUND(HttpStatus.BAD_REQUEST, "CAREER_LEVEL_NOT_FOUND", "경력 레벨을 찾을 수 없습니다."),
    CAREER_LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "CAREER_LEVEL_REQUIRED", "경력 정보가 필요합니다."),

    /* =======================
     * Expert
     * ======================= */
    EXPERT_USER_ID_INVALID(HttpStatus.BAD_REQUEST, "EXPERT_USER_ID_INVALID", "유효하지 않은 현직자 ID입니다."),
    EXPERT_EMBEDDING_EMPTY(HttpStatus.BAD_REQUEST, "EXPERT_EMBEDDING_EMPTY", "임베딩 벡터가 비어 있습니다."),
    EXPERT_FILTER_INVALID(HttpStatus.BAD_REQUEST, "EXPERT_FILTER_INVALID", "필터 파라미터가 올바르지 않습니다."),
    EXPERT_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPERT_NOT_FOUND", "현직자를 찾을 수 없습니다."),

    /* =======================
     * Email Verification
     * ======================= */
    EMAIL_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "EMAIL_FORMAT_INVALID", "이메일 형식이 올바르지 않습니다."),
    EMAIL_DOMAIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EMAIL_DOMAIN_NOT_ALLOWED", "인증 불가한 이메일 도메인입니다."),
    EMAIL_NOT_COMPANY_EMAIL(HttpStatus.BAD_REQUEST, "EMAIL_NOT_COMPANY_EMAIL", "회사 이메일만 사용할 수 있습니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "이미 인증이 완료된 이메일입니다."),
    EMAIL_VERIFICATION_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "EMAIL_VERIFICATION_RATE_LIMIT", "인증 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_INVALID", "인증 코드가 올바르지 않습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_MISMATCH", "인증 코드가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.GONE, "VERIFICATION_CODE_EXPIRED", "인증 코드가 만료되었습니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_REQUIRED", "이메일 인증이 필요합니다."),
    EMAIL_VERIFICATION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다."),

    /* =======================
     * Resume
     * ======================= */
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND", "이력서를 찾을 수 없습니다."),
    RESUME_ID_INVALID(HttpStatus.BAD_REQUEST, "RESUME_ID_INVALID", "유효하지 않은 이력서 ID입니다."),
    RESUME_IS_FRESHER_INVALID(HttpStatus.BAD_REQUEST, "RESUME_IS_FRESHER_INVALID", "신입 여부 값이 올바르지 않습니다."),
    RESUME_EDUCATION_LEVEL_INVALID(HttpStatus.BAD_REQUEST, "RESUME_EDUCATION_LEVEL_INVALID", "학력 값이 올바르지 않습니다."),
    RESUME_CONTENT_INVALID(HttpStatus.BAD_REQUEST, "RESUME_CONTENT_INVALID", "이력서 내용이 올바르지 않습니다."),
    RESUME_TITLE_EMPTY(HttpStatus.BAD_REQUEST, "RESUME_TITLE_EMPTY", "이력서 제목을 입력해 주세요."),
    RESUME_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "RESUME_TITLE_TOO_LONG", "이력서 제목이 너무 깁니다."),
    RESUME_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "RESUME_LIMIT_EXCEEDED", "이력서 개수가 제한을 초과했습니다."),

    RESUME_FILE_NOT_PDF(HttpStatus.BAD_REQUEST, "RESUME_FILE_NOT_PDF", "이력서 파일은 PDF만 가능합니다."),
    RESUME_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "RESUME_FILE_TOO_LARGE", "이력서 파일 용량이 너무 큽니다."),
    RESUME_FILE_URL_INVALID(HttpStatus.BAD_REQUEST, "RESUME_FILE_URL_INVALID", "이력서 파일 URL이 올바르지 않습니다."),
    RESUME_MODE_INVALID(HttpStatus.BAD_REQUEST, "RESUME_MODE_INVALID", "파싱 모드는 sync 또는 async여야 합니다."),
    RESUME_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RESUME_PARSE_FAILED", "이력서 파싱에 실패했습니다."),

    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "작업을 찾을 수 없습니다."),

    /* =======================
     * Chat
     * ======================= */
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND", "채팅을 찾을 수 없습니다."),
    CHAT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "CHAT_ALREADY_CLOSED", "종료된 채팅에는 메시지를 보낼 수 없습니다."),
    MESSAGE_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "MESSAGE_CONTENT_EMPTY", "메시지 내용을 입력해 주세요."),
    MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "MESSAGE_CONTENT_TOO_LONG", "메시지 내용이 너무 깁니다."),
    CHAT_RECEIVER_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_RECEIVER_REQUIRED", "수신자 ID가 필요합니다."),
    CHAT_REQUEST_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_REQUEST_TYPE_REQUIRED", "요청 타입이 필요합니다."),
    CHAT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_ID_REQUIRED", "채팅 ID가 필요합니다."),
    CHAT_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT_STATUS_REQUIRED", "채팅 상태가 필요합니다."),
    MESSAGE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "MESSAGE_ID_REQUIRED", "메시지 ID가 필요합니다."),
    CHAT_JOB_POST_URL_TOO_LONG(HttpStatus.BAD_REQUEST, "CHAT_JOB_POST_URL_TOO_LONG", "공고 링크가 너무 깁니다."),

    CHAT_STATUS_INVALID(HttpStatus.BAD_REQUEST, "CHAT_STATUS_INVALID", "상태는 ACCEPTED 또는 REJECTED여야 합니다."),
    CHAT_ALREADY_RESPONDED(HttpStatus.BAD_REQUEST, "CHAT_ALREADY_RESPONDED", "이미 응답된 채팅 요청입니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHAT_ROOM_ALREADY_EXISTS", "채팅방이 이미 존재합니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다."),
    /* =======================
     * Review / Feedback
     * ======================= */
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW_ALREADY_EXISTS", "이미 리뷰가 제출되었습니다."),
    REVIEW_RATING_INVALID(HttpStatus.BAD_REQUEST, "REVIEW_RATING_INVALID", "평점은 1~5 사이여야 합니다."),
    REVIEW_COMMENT_TOO_LONG(HttpStatus.BAD_REQUEST, "REVIEW_COMMENT_TOO_LONG", "리뷰 내용이 너무 깁니다."),
    CHAT_NOT_CLOSED(HttpStatus.BAD_REQUEST, "CHAT_NOT_CLOSED", "채팅 종료 후에만 리뷰할 수 있습니다."),

    FEEDBACK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FEEDBACK_ALREADY_EXISTS", "이미 피드백이 제출되었습니다."),
    FEEDBACK_ANSWER_INVALID(HttpStatus.BAD_REQUEST, "FEEDBACK_ANSWER_INVALID", "피드백 응답이 올바르지 않습니다."),
    FEEDBACK_ANSWER_MISSING(HttpStatus.BAD_REQUEST, "FEEDBACK_ANSWER_MISSING", "필수 질문에 대한 응답이 없습니다."),

    /* =======================
     * Notification
     * ======================= */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    NOTIFICATION_ALREADY_READ(HttpStatus.CONFLICT, "NOTIFICATION_ALREADY_READ", "이미 읽은 알림입니다."),
    NOTIFICATION_IS_READ_INVALID(HttpStatus.BAD_REQUEST, "NOTIFICATION_IS_READ_INVALID", "읽음 여부 값이 올바르지 않습니다."),

    /* =======================
     * Report
     * ======================= */
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "리포트를 찾을 수 없습니다."),
    REPORT_ALREADY_PROCESSING(HttpStatus.CONFLICT, "REPORT_ALREADY_PROCESSING", "리포트 생성이 이미 진행 중입니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "REPORT_ALREADY_EXISTS", "해당 채팅의 리포트가 이미 존재합니다."),
    REPORT_STATUS_NOT_FAILED(HttpStatus.BAD_REQUEST, "REPORT_STATUS_NOT_FAILED", "실패한 리포트만 재시도할 수 있습니다."),
    REPORT_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT_DELETE_NOT_ALLOWED", "현재 상태에서는 리포트를 삭제할 수 없습니다."),

    REPORT_IDS_EMPTY(HttpStatus.BAD_REQUEST, "REPORT_IDS_EMPTY", "리포트 ID가 필요합니다."),
    REPORT_IDS_TOO_MANY(HttpStatus.BAD_REQUEST, "REPORT_IDS_TOO_MANY", "리포트 ID가 너무 많습니다."),

    /* =======================
     * AI Chat
     * ======================= */
    AI_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_CHAT_NOT_FOUND", "AI 채팅을 찾을 수 없습니다."),
    AI_CHAT_PURPOSE_REQUIRED(HttpStatus.BAD_REQUEST, "AI_CHAT_PURPOSE_REQUIRED", "목적이 필요합니다."),
    AI_CHAT_PURPOSE_INVALID(HttpStatus.BAD_REQUEST, "AI_CHAT_PURPOSE_INVALID", "목적이 올바르지 않습니다."),
    AI_CHAT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "AI_CHAT_ALREADY_CLOSED", "종료된 AI 채팅에는 메시지를 보낼 수 없습니다."),
    AI_CHAT_STATUS_INVALID(HttpStatus.BAD_REQUEST, "AI_CHAT_STATUS_INVALID", "상태는 CLOSED여야 합니다."),




    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청이 올바르지 않습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST,"INVALID_JSON_REQUEST", "요청 JSON이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "AI 서버 오류가 발생했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
