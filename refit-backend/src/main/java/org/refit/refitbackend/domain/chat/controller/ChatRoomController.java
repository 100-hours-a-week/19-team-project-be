package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.Parameter;
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
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.swagger.spec.chat.ChatSwaggerSpec;
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
    @ChatSwaggerSpec.CreateRoom
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
    @ChatSwaggerSpec.ListRooms
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
    @ChatSwaggerSpec.GetRoomDetail
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
     * 채팅방 이력서 원본 다운로드용 presigned URL 발급
     */
    @ChatSwaggerSpec.GetResumeDownloadUrl
    @GetMapping("/{chat_id}/resume/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getResumePresignedUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "채팅방 ID", example = "1", required = true)
            @PathVariable("chat_id") Long chatId
    ) {
        PresignedUrlResponse response = chatRoomService.getResumeDownloadUrl(principal.getUserId(), chatId);
        return ResponseUtil.ok("presigned_url_issued", response);
    }

    /**
     * 채팅방 종료
     */
    @ChatSwaggerSpec.CloseRoom
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
    @ChatSwaggerSpec.GetMessages
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
    @ChatSwaggerSpec.MarkAsRead
    @PatchMapping("/{chat_id}/last-read-message")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "채팅방 ID", example = "1", required = true)
            @PathVariable("chat_id") Long chatId,
            @Valid @RequestBody ChatReq.ReadMessage request
    ) {
        chatRoomService.markAsRead(principal.getUserId(), chatId, request);
        return ResponseUtil.ok("success", null);
    }
}
