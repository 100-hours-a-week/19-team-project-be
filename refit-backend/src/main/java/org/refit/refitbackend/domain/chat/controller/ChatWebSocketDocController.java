package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.response.ErrorResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/docs/ws")
@Tag(name = "WebSocket", description = "WebSocket (STOMP) 메시지 API 문서")
public class ChatWebSocketDocController {

    @Operation(
            summary = "채팅 메시지 전송 (WebSocket)",
            description = """
                STOMP 엔드포인트 매핑
                - 전송 주소: /app/chat.sendMessage
                - 구독 주소: /queue/chat.{chat_id}
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "created_success",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "created_success",
                                    value = "{ \"code\": \"CREATED\", \"message\": \"created_success\", \"data\": { \"chat_id\": 1 } }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "broadcast_message",
                    content = @Content(
                            schema = @Schema(implementation = ChatRes.MessageInfo.class),
                            examples = @ExampleObject(
                                    name = "broadcast_message",
                                    value = "{ \"message_id\": 100, \"chat_id\": 1, \"sender\": { \"user_id\": 2, \"nickname\": \"eden\", \"profile_image_url\": \"https://cdn.refit.com/default-profile.png\", \"user_type\": \"JOB_SEEKER\" }, \"message_type\": \"TEXT\", \"content\": \"안녕하세요\", \"created_at\": \"2026-01-22 16:07:21\" }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_request",
                                    value = "{ \"code\": \"INVALID_REQUEST\", \"message\": \"invalid_request\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "unauthorized",
                                    value = "{ \"code\": \"AUTH_UNAUTHORIZED\", \"message\": \"unauthorized\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "forbidden",
                                    value = "{ \"code\": \"FORBIDDEN\", \"message\": \"forbidden\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "chat_not_found",
                                    value = "{ \"code\": \"CHAT_NOT_FOUND\", \"message\": \"chat_not_found\", \"data\": null }"
                            )
                    )
            )
    })
    @PostMapping("/chat/send")
    public ResponseEntity<ApiResponse<ChatRes.MessageInfo>> sendMessageDoc(
            @RequestBody ChatReq.SendMessage request
    ) {
        // Documentation-only endpoint (does not send a message).
        return ResponseUtil.created("created_success", null);
    }
}
