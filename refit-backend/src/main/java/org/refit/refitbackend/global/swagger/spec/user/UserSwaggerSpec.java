package org.refit.refitbackend.global.swagger.spec.user;

import org.refit.refitbackend.domain.user.dto.UserReq;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class UserSwaggerSpec {

    private UserSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "유저 단건 조회",
            operationDescription = "유저 ID로 단건 조회",
            implementation = UserRes.Detail.class
    )
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiNotFoundError(description = "user_not_found", types = { ExceptionType.USER_NOT_FOUND })
    public @interface GetUser {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "내 정보 조회",
            operationDescription = "로그인한 사용자 정보 조회",
            implementation = UserRes.Me.class
    )
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface GetMe {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "닉네임 중복 검사",
            operationDescription = "회원가입 / 내정보 수정 시 닉네임 중복 검사",
            implementation = UserRes.NicknameCheck.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.NICKNAME_EMPTY,
            ExceptionType.NICKNAME_TOO_SHORT,
            ExceptionType.NICKNAME_TOO_LONG,
            ExceptionType.NICKNAME_INVALID_CHARACTERS,
            ExceptionType.NICKNAME_CONTAINS_WHITESPACE
    })
    public @interface CheckNickname {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "유저 검색",
            operationDescription = "전체 유저를 직무, 스킬, 키워드로 검색합니다",
            implementation = UserRes.UserCursorResponse.class
    )
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface SearchUsers {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "내 정보 수정",
            operationDescription = "프로필/자기소개/직무/스킬/경력을 부분 수정합니다.",
            implementation = UserRes.Me.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.NICKNAME_EMPTY,
            ExceptionType.NICKNAME_TOO_SHORT,
            ExceptionType.NICKNAME_TOO_LONG,
            ExceptionType.NICKNAME_INVALID_CHARACTERS,
            ExceptionType.NICKNAME_CONTAINS_WHITESPACE,
            ExceptionType.NICKNAME_DUPLICATE,
            ExceptionType.CAREER_LEVEL_NOT_FOUND,
            ExceptionType.JOB_NOT_FOUND,
            ExceptionType.JOB_DUPLICATE,
            ExceptionType.JOB_IDS_EMPTY,
            ExceptionType.SKILL_NOT_FOUND,
            ExceptionType.SKILL_DUPLICATE,
            ExceptionType.SKILL_IDS_EMPTY,
            ExceptionType.SKILL_DISPLAY_ORDER_REQUIRED,
            ExceptionType.IMAGE_URL_INVALID
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = UserReq.UpdateMe.class,
            examples = {
                    "{ \"nickname\": \"eden\", \"introduction\": \"백엔드 개발자입니다.\", \"profile_image_url\": \"https://...\", \"career_level_id\": 1, \"job_ids\": [1,2], \"skills\": [{\"skill_id\": 1, \"display_order\": 1}] }"
            },
            exampleNames = { "update_me" }
    )
    public @interface UpdateMe {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "현직자 인증 상태 조회",
            operationDescription = "현직자 인증 여부/정보를 조회합니다.",
            implementation = UserRes.ExpertVerificationStatus.class
    )
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface GetExpertVerificationStatus {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "유저 타입 전환(개발용)",
            operationDescription = "JOB_SEEKER ↔ EXPERT 전환",
            implementation = Void.class
    )
    public @interface ChangeUserTypeDev {}
}
