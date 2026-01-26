package org.refit.refitbackend.global.swagger.spec.expert;

import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ExpertSwaggerSpec {

    private ExpertSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "현직자 목록 조회",
            operationDescription = "키워드/필터로 현직자 검색 (커서 기반)",
            implementation = ExpertRes.ExpertCursorResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.EXPERT_FILTER_INVALID,
            ExceptionType.INVALID_CURSOR
    })
    public @interface SearchExperts {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "현직자 상세 조회",
            operationDescription = "현직자 상세 프로필 조회",
            implementation = ExpertRes.ExpertDetail.class
    )
    @SwaggerApiNotFoundError(description = "expert_not_found", types = {
            ExceptionType.EXPERT_NOT_FOUND
    })
    public @interface GetExpertDetail {}
}
