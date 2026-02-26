package org.refit.refitbackend.global.swagger.spec.expert;

import org.refit.refitbackend.domain.expert.dto.ExpertReq;
import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ExpertInternalSwaggerSpec {

    private ExpertInternalSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Internal 현직자 임베딩 저장",
            operationDescription = "AI 서버가 생성한 현직자 임베딩 벡터를 내부 전용 API로 저장합니다.",
            implementation = Void.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.EXPERT_USER_ID_INVALID,
            ExceptionType.EXPERT_EMBEDDING_EMPTY
    })
    @SwaggerApiNotFoundError(description = "expert_not_found", types = {
            ExceptionType.EXPERT_NOT_FOUND
    })
    @SwaggerApiRequestBody(
            implementation = ExpertReq.UpdateEmbedding.class,
            examples = { "{ \"user_id\": 1, \"embedding\": [0.12, -0.34, 0.56] }" },
            exampleNames = { "internal_update_embedding" }
    )
    public @interface UpdateEmbeddingInternal {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Internal 멘토 임베딩 재생성 요청",
            operationDescription = "특정 멘토의 임베딩 재생성을 AI 서버에 요청하는 내부 전용 API입니다.",
            implementation = ExpertRes.MentorEmbeddingUpdateResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.EXPERT_USER_ID_INVALID
    })
    @SwaggerApiNotFoundError(description = "expert_not_found", types = {
            ExceptionType.EXPERT_NOT_FOUND
    })
    @SwaggerApiError(responseCode = "500", description = "ai_server_error", types = {
            ExceptionType.AI_SERVER_ERROR
    })
    public @interface RefreshMentorEmbeddingInternal {}
}
