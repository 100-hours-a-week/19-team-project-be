package org.refit.refitbackend.global.swagger.spec.auth;

import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationReq;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.annotation.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class AuthSwaggerSpec {

    private AuthSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "회원가입",
            operationDescription = """
                OAuth 정보 + 추가 정보로 회원가입합니다.

                - JOB_SEEKER: 회사 정보 없이 가입 가능
                - EXPERT: company_email을 보낼 수 있으며, 인증 완료된 이메일이면 verified=true로 저장
                - 인증되지 않은 이메일이거나 미입력 시 verified=false로 가입됩니다.
            """,
            responseCode = "201",
            implementation = AuthRes.LoginSuccess.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.SIGNUP_OAUTH_PROVIDER_INVALID,
            ExceptionType.SIGNUP_OAUTH_ID_EMPTY,
            ExceptionType.SIGNUP_EMAIL_INVALID,
            ExceptionType.SIGNUP_USER_TYPE_INVALID,
            ExceptionType.EMAIL_VERIFICATION_REQUIRED,
            ExceptionType.EMAIL_VERIFICATION_NOT_VERIFIED
    })
    @SwaggerApiConflictError(types = {
            ExceptionType.SIGNUP_ALREADY_EXISTS,
            ExceptionType.EMAIL_DUPLICATE,
            ExceptionType.OAUTH_DUPLICATE
    })
    @SwaggerApiRequestBody(
            implementation = AuthReq.SignUp.class,
            examples = {
                    "{ \"oauth_provider\": \"KAKAO\", \"oauth_id\": \"123456\", \"email\": \"user@kakao.com\", \"nickname\": \"eden\", \"user_type\": \"JOB_SEEKER\", \"career_level_id\": 1, \"job_ids\": [1, 2], \"skills\": [{\"skill_id\": 1, \"display_order\": 1}], \"introduction\": \"백엔드 개발자입니다.\" }",
                    "{ \"oauth_provider\": \"KAKAO\", \"oauth_id\": \"123456\", \"email\": \"user@kakao.com\", \"nickname\": \"eden\", \"user_type\": \"EXPERT\", \"career_level_id\": 1, \"job_ids\": [1], \"skills\": [{\"skill_id\": 1, \"display_order\": 1}], \"introduction\": \"백엔드 개발자입니다.\",  \"company_email\": \"user@navercorp.com\" }"
            },
            exampleNames = { "job_seeker", "expert_unverified" }
    )
    public @interface Signup {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "카카오 로그인",
            operationDescription = """
                카카오 OAuth 로그인 처리
                - 기존 회원: 로그인 성공
                - 신규 회원: 회원가입 필요 데이터 반환
            """,
            implementation = AuthRes.OAuthLoginResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_AUTH_CODE,
            ExceptionType.AUTH_CODE_REQUIRED
    })
    public @interface KakaoLogin {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "카카오 로그인(로컬)",
            operationDescription = "카카오 OAuth 로그인 처리(로컬)",
            implementation = AuthRes.OAuthLoginResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_AUTH_CODE,
            ExceptionType.AUTH_CODE_REQUIRED
    })
    public @interface KakaoLoginLocal {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "토큰 재발급",
            operationDescription = "RTR 방식으로 AT/RT 재발급",
            responseCode = "201",
            responseDescription = "created",
            implementation = ApiResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.AUTH_INVALID_REQUEST,
            ExceptionType.REFRESH_TOKEN_REQUIRED
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED,
            ExceptionType.AUTH_UNAUTHORIZED
    })
    public @interface RefreshTokens {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "카카오 로그인 페이지로 리다이렉트",
            operationDescription = "카카오 OAuth 인증 페이지로 리다이렉트",
            responseCode = "302",
            implementation = Void.class,
            wrapApiResponse = false
    )
    public @interface KakaoAuthorize {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "카카오 로그인 페이지로 리다이렉트(로컬)",
            operationDescription = "카카오 OAuth 인증 페이지로 리다이렉트(로컬)",
            responseCode = "302",
            implementation = Void.class,
            wrapApiResponse = false
    )
    public @interface KakaoAuthorizeLocal {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "개발용 토큰 발급",
            operationDescription = "사용자 ID로 AT/RT 발급 (개발 환경용)",
            implementation = AuthRes.TokenDto.class
    )
    public @interface DevToken {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이메일 인증 코드 발송",
            operationDescription = "회사 이메일로 6자리 인증번호를 발송합니다.",
            implementation = EmailVerificationRes.Send.class,
            responseDescription = "verification_code_sent"
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.EMAIL_FORMAT_INVALID,
            ExceptionType.EMAIL_DOMAIN_NOT_ALLOWED,
            ExceptionType.EMAIL_NOT_COMPANY_EMAIL
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    @SwaggerApiError(responseCode = "429", description = "rate_limit", types = {
            ExceptionType.EMAIL_VERIFICATION_RATE_LIMIT
    })
    @SwaggerApiError(responseCode = "409", description = "already_verified", types = {
            ExceptionType.EMAIL_ALREADY_VERIFIED
    })
    @SwaggerApiRequestBody(
            implementation = EmailVerificationReq.Send.class,
            examples = { "{ \"email\": \"user@navercorp.com\" }" },
            exampleNames = { "send_request" }
    )
    public @interface SendEmailVerificationCode {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이메일 인증 코드 발송 (회원가입용)",
            operationDescription = "회원가입 단계에서 이메일로 6자리 인증번호를 발송합니다.",
            implementation = EmailVerificationRes.Send.class,
            responseDescription = "verification_code_sent"
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.EMAIL_FORMAT_INVALID,
            ExceptionType.EMAIL_DOMAIN_NOT_ALLOWED,
            ExceptionType.EMAIL_NOT_COMPANY_EMAIL
    })
    @SwaggerApiError(responseCode = "429", description = "rate_limit", types = {
            ExceptionType.EMAIL_VERIFICATION_RATE_LIMIT
    })
    @SwaggerApiError(responseCode = "409", description = "already_verified", types = {
            ExceptionType.EMAIL_ALREADY_VERIFIED
    })
    @SwaggerApiRequestBody(
            implementation = EmailVerificationReq.Send.class,
            examples = { "{ \"email\": \"user@navercorp.com\" }" },
            exampleNames = { "send_request" }
    )
    public @interface SendEmailVerificationCodePublic {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이메일 인증 코드 확인",
            operationDescription = "발송된 인증번호를 검증하고 인증을 완료합니다.",
            implementation = EmailVerificationRes.Verify.class,
            responseDescription = "verification_success"
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.VERIFICATION_CODE_INVALID,
            ExceptionType.VERIFICATION_CODE_MISMATCH,
            ExceptionType.EMAIL_VERIFICATION_REQUIRED,
            ExceptionType.EMAIL_VERIFICATION_NOT_VERIFIED
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    @SwaggerApiError(responseCode = "410", description = "verification_code_expired", types = {
            ExceptionType.VERIFICATION_CODE_EXPIRED
    })
    @SwaggerApiRequestBody(
            implementation = EmailVerificationReq.Verify.class,
            examples = { "{ \"email\": \"user@navercorp.com\", \"code\": \"123456\" }" },
            exampleNames = { "verify_request" }
    )
    public @interface VerifyEmailCode {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이메일 인증 코드 확인 (회원가입용)",
            operationDescription = "회원가입 단계에서 발송된 인증번호를 검증합니다.",
            implementation = EmailVerificationRes.Verify.class,
            responseDescription = "verification_success"
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.VERIFICATION_CODE_INVALID,
            ExceptionType.VERIFICATION_CODE_MISMATCH
    })
    @SwaggerApiError(responseCode = "410", description = "verification_code_expired", types = {
            ExceptionType.VERIFICATION_CODE_EXPIRED
    })
    @SwaggerApiRequestBody(
            implementation = EmailVerificationReq.Verify.class,
            examples = { "{ \"email\": \"user@navercorp.com\", \"code\": \"123456\" }" },
            exampleNames = { "verify_request" }
    )
    public @interface VerifyEmailCodePublic {}
}
