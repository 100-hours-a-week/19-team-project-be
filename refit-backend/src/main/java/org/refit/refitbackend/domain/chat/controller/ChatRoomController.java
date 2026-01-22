package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.service.ChatRoomService;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.response.ErrorResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
@Tag(name = "ChatRoom", description = "채팅방 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 채팅방 생성
     */
    @Operation(summary = "채팅방 생성", description = "현직자와의 채팅방을 생성합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "created",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "created",
                                    value = "{ \"code\": \"CREATED\", \"message\": \"create_success\", \"data\": { \"chat_id\": 1 } }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
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
                    description = "unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "unauthorized",
                                    value = "{ \"code\": \"AUTH_UNAUTHORIZED\", \"message\": \"unauthorized\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "chat_room_already_exists",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "chat_room_already_exists",
                                    value = "{ \"code\": \"CHAT_ROOM_ALREADY_EXISTS\", \"message\": \"chat_room_already_exists\", \"data\": null }"
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRes.CreateChat>> createRoom(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChatReq.CreateRoom request
    ) {
        ChatRes.CreateChat chatId = chatRoomService.createRoom(principal.getUserId(), request);
        return ResponseUtil.created("create_success", chatId);
    }

    /**
     * 내 채팅방 목록 조회
     */
    @Operation(summary = "채팅방 목록 조회", description = "내가 참여 중인 채팅방 목록을 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "success",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = "{ \"code\": \"OK\", \"message\": \"success\", \"data\": { \"chats\": [], \"nextCursor\": null, \"hasMore\": false } }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "unauthorized",
                                    value = "{ \"code\": \"AUTH_UNAUTHORIZED\", \"message\": \"unauthorized\", \"data\": null }"
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatCursorResponse>> getChats(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "채팅방 상태", example = "ACTIVE")
            @RequestParam(defaultValue = "ACTIVE") ChatRoomStatus status,
            @Parameter(description = "커서(마지막 채팅방 ID)", example = "100")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPage<ChatRes.RoomListItem> page = chatRoomService.getMyChats(principal.getUserId(), status, cursor, size);
        ChatRes.ChatCursorResponse res = new ChatRes.ChatCursorResponse(page.items(), page.nextCursor(), page.hasMore());
        return ResponseUtil.ok("success", res);
    }

    /**
     * 채팅방 상세 조회
     */
    @Operation(summary = "채팅방 상세 조회", description = "채팅방 상세 정보를 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "success",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = "{ \"code\": \"OK\", \"message\": \"success\", \"data\": { \"chatId\": 1 } }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "unauthorized",
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
                    description = "forbidden",
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
                    description = "chat_not_found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "chat_not_found",
                                    value = "{ \"code\": \"CHAT_NOT_FOUND\", \"message\": \"chat_not_found\", \"data\": null }"
                            )
                    )
            )
    })
    @GetMapping("/{chat_id}")
    public ResponseEntity<ApiResponse<ChatRes.RoomDetail>> getRoomDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "채팅방 ID", example = "1", required = true)
            @PathVariable("chat_id") Long chatId
    ) {
        ChatRes.RoomDetail room = chatRoomService.getRoomDetail(principal.getUserId(), chatId);
        return ResponseUtil.ok("success", room);
    }

    /**
     * 채팅방 종료
     */
    @Operation(summary = "채팅방 종료", description = "채팅방을 종료합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "success",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = "{ \"code\": \"OK\", \"message\": \"success\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "chat_already_closed",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "chat_already_closed",
                                    value = "{ \"code\": \"CHAT_ALREADY_CLOSED\", \"message\": \"cannot send message to closed chat\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "unauthorized",
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
                    description = "forbidden",
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
                    description = "chat_not_found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "chat_not_found",
                                    value = "{ \"code\": \"CHAT_NOT_FOUND\", \"message\": \"chat_not_found\", \"data\": null }"
                            )
                    )
            )
    })
    @PatchMapping("/{chat_id}")
    public ResponseEntity<ApiResponse<Void>> closeRoom(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "채팅방 ID", example = "1", required = true)
            @PathVariable("chat_id") Long chatId,
            @Valid @RequestBody ChatReq.CloseRoom request
    ) {
        if (!"CLOSED".equalsIgnoreCase(request.status())) {
            return ResponseUtil.error(ExceptionType.INVALID_REQUEST);
        }
        chatRoomService.closeRoom(principal.getUserId(), chatId);
        return ResponseUtil.ok("success", null);
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @Operation(summary = "메시지 목록 조회", description = "채팅방의 메시지 목록을 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "success",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = "{ \"code\": \"OK\", \"message\": \"success\", \"data\": { \"messages\": [], \"nextCursor\": null, \"hasMore\": false } }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "unauthorized",
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
                    description = "forbidden",
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
                    description = "chat_not_found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "chat_not_found",
                                    value = "{ \"code\": \"CHAT_NOT_FOUND\", \"message\": \"chat_not_found\", \"data\": null }"
                            )
                    )
            )
    })
    @GetMapping("/{chat_id}/messages")
    public ResponseEntity<ApiResponse<ChatRes.MessageCursorResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "채팅방 ID", example = "1", required = true)
            @PathVariable("chat_id") Long chatId,
            @Parameter(description = "커서(마지막 메시지 ID)", example = "200")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기", example = "50")
            @RequestParam(defaultValue = "50") int size
    ) {
        CursorPage<ChatRes.MessageInfo> page = chatRoomService.getMessages(principal.getUserId(), chatId, cursor, size);
        ChatRes.MessageCursorResponse res = new ChatRes.MessageCursorResponse(page.items(), page.nextCursor(), page.hasMore());
        return ResponseUtil.ok("success", res);
    }

    /**
     * 메시지 읽음 처리
     */
    @Operation(summary = "메시지 읽음 처리", description = "메시지를 읽음 처리합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "success",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = "{ \"code\": \"OK\", \"message\": \"success\", \"data\": null }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
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
                    description = "unauthorized",
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
                    description = "forbidden",
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
                    description = "message_not_found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "message_not_found",
                                    value = "{ \"code\": \"MESSAGE_NOT_FOUND\", \"message\": \"message_not_found\", \"data\": null }"
                            )
                    )
            )
    })
    @PatchMapping("/messages/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChatReq.ReadMessage request
    ) {
        chatRoomService.markAsRead(principal.getUserId(), request);
        return ResponseUtil.ok("success", null);
    }
}
