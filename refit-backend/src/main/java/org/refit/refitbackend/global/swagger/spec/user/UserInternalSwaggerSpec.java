package org.refit.refitbackend.global.swagger.spec.user;

import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class UserInternalSwaggerSpec {

    private UserInternalSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Internal 유저 단건 조회",
            operationDescription = "AI/내부 서비스 연동용 유저 단건 조회 API입니다.",
            implementation = UserRes.Detail.class
    )
    @SwaggerApiNotFoundError(description = "user_not_found", types = {
            ExceptionType.USER_NOT_FOUND
    })
    public @interface GetUserInternal {}
}
