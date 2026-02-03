package org.refit.refitbackend.global.swagger.spec.chat;

import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ChatSwaggerSpec {

    private ChatSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "채팅방 생성",
            operationDescription = "현직자와의 채팅방을 생성합니다",
            responseCode = "201",
            responseDescription = "created",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "409", description = "chat_room_already_exists", types = {
            ExceptionType.CHAT_ROOM_ALREADY_EXISTS
    })
    public @interface CreateRoom {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "채팅방 목록 조회",
            operationDescription = "내가 참여 중인 채팅방 목록을 조회합니다",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    public @interface ListRooms {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "채팅방 상세 조회",
            operationDescription = "채팅방 상세 정보를 조회합니다",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.FORBIDDEN,
            ExceptionType.AUTH_FORBIDDEN
    })
    @SwaggerApiError(responseCode = "404", description = "chat_not_found", types = {
            ExceptionType.CHAT_NOT_FOUND
    })
    public @interface GetRoomDetail {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "채팅방 이력서 다운로드 URL 발급",
            operationDescription = "채팅방에 연결된 이력서 원본 다운로드용 presigned URL을 발급합니다",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.STORAGE_ACCESS_FORBIDDEN
    })
    @SwaggerApiError(responseCode = "404", description = "not_found", types = {
            ExceptionType.CHAT_ROOM_NOT_FOUND,
            ExceptionType.RESUME_NOT_FOUND
    })
    public @interface GetResumeDownloadUrl {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "채팅방 종료",
            operationDescription = "채팅방을 종료합니다",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "400", description = "chat_already_closed", types = {
            ExceptionType.CHAT_ALREADY_CLOSED
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.FORBIDDEN,
            ExceptionType.AUTH_FORBIDDEN
    })
    @SwaggerApiError(responseCode = "404", description = "chat_not_found", types = {
            ExceptionType.CHAT_NOT_FOUND
    })
    public @interface CloseRoom {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "메시지 목록 조회",
            operationDescription = "채팅방의 메시지 목록을 조회합니다",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.FORBIDDEN,
            ExceptionType.AUTH_FORBIDDEN
    })
    @SwaggerApiError(responseCode = "404", description = "chat_not_found", types = {
            ExceptionType.CHAT_NOT_FOUND
    })
    public @interface GetMessages {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "메시지 읽음 처리",
            operationDescription = "채팅방의 마지막 읽은 메시지를 갱신합니다",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.INVALID_REQUEST,
            ExceptionType.MESSAGE_ID_REQUIRED
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.FORBIDDEN,
            ExceptionType.AUTH_FORBIDDEN
    })
    @SwaggerApiError(responseCode = "404", description = "message_not_found", types = {
            ExceptionType.MESSAGE_NOT_FOUND,
            ExceptionType.CHAT_ROOM_NOT_FOUND
    })
    public @interface MarkAsRead {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "채팅 메시지 전송 (WebSocket)",
            operationDescription = """
                STOMP 엔드포인트 매핑
                - 전송 주소: /app/chat.sendMessage
                - 구독 주소: /queue/chat.{chat_id}
                """,
            responseCode = "201",
            responseDescription = "created_success",
            implementation = ApiResponse.class
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.FORBIDDEN,
            ExceptionType.AUTH_FORBIDDEN
    })
    @SwaggerApiError(responseCode = "404", description = "chat_not_found", types = {
            ExceptionType.CHAT_NOT_FOUND
    })
    public @interface WebSocketSendMessageDoc {}
}
