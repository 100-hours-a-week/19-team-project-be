package org.refit.refitbackend.global.swagger.spec.agent;

import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.domain.agent.dto.AgentReq;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class AgentSwaggerSpec {

    private AgentSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Agent 세션 생성",
            operationDescription = "새 Agent 채팅 세션을 생성합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    public @interface CreateSession {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Agent 세션 목록 조회",
            operationDescription = "현재 활성화된 Agent 세션 목록을 조회합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    public @interface ListSessions {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Agent 세션 단건 조회",
            operationDescription = "session_id로 특정 Agent 세션을 조회합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "404", description = "not found", types = {
            ExceptionType.AI_CHAT_NOT_FOUND
    })
    public @interface GetSession {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Agent 세션 메시지 조회",
            operationDescription = "session_id에 저장된 사용자/어시스턴트 메시지 이력을 조회합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "404", description = "not found", types = {
            ExceptionType.AI_CHAT_NOT_FOUND
    })
    public @interface GetMessages {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Agent 답변 스트리밍",
            operationDescription = """
                    사용자 메시지를 전달하고 SSE(text/event-stream)로 Agent 이벤트를 스트리밍합니다.
                    - 이벤트: session, intent, conditions, cards, text, done, error
                    - session_id 미전달 시 서버에서 새 세션을 생성합니다.
                    """,
            implementation = String.class,
            wrapApiResponse = false
    )
    @SwaggerApiRequestBody(
            implementation = AgentReq.ReplyRequest.class,
            examples = {
                    "{ \"session_id\": \"ad6ebada-fcfe-4027-b2fc-ed45bf188982\", \"message\": \"백엔드 현직자 누구있어?\", \"top_k\": 3 }",
                    "{ \"message\": \"Spring Boot, AWS, Redis, Kafka에 대해 능숙한 현직자 추천해줘\", \"top_k\": 3 }"
            },
            exampleNames = {"with_session", "without_session"}
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    public @interface ReplyStream {}
}
