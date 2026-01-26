package org.refit.refitbackend.global.swagger.spec.user;

import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
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
            summary = "유저 타입 전환(개발용)",
            operationDescription = "JOB_SEEKER ↔ EXPERT 전환",
            implementation = Void.class
    )
    public @interface ChangeUserTypeDev {}
}
