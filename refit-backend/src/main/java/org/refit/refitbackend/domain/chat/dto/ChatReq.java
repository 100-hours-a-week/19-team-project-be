package org.refit.refitbackend.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatReq {

    @Schema(description = "채팅방 생성 요청")
    public record CreateRoom(
            @Schema(description = "수신자(현직자) ID", example = "2")
            @NotNull(message = "chat_receiver_required")
            Long receiverId,

            @Schema(description = "이력서 ID (선택)", example = "1")
            Long resumeId,

            @Schema(description = "공고 URL (선택)", example = "https://example.com/job/123")
            @Size(max = 500, message = "chat_job_post_url_too_long")
            String jobPostUrl,

            @Schema(description = "채팅 요청 유형", example = "FEEDBACK / COFFEE_CHAT")
            @NotNull(message = "chat_request_type_required")
            String requestType
    ) {}

    @Schema(description = "메시지 전송 요청 (WebSocket)")
    public record SendMessage(
            @Schema(description = "채팅방 ID", example = "1")
            @NotNull(message = "chat_id_required")
            Long chatId,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            @NotNull(message = "message_content_required")
            @Size(min = 1, max = 500, message = "message_content_length_invalid")
            String content,

            @Schema(description = "메시지 타입", example = "TEXT")
            String messageType  // TEXT, FILE, SYSTEM
    ) {}

    @Schema(description = "메시지 읽음 처리 요청")
    public record ReadMessage(
            @Schema(description = "채팅방 ID", example = "1")
            @NotNull(message = "chat_id_required")
            Long chatId,

            @Schema(description = "읽은 메시지 ID", example = "100")
            @NotNull(message = "message_id_required")
            Long messageId
    ) {}

    @Schema(description = "채팅방 종료 요청")
    public record CloseRoom(
            @Schema(description = "채팅방 상태", example = "CLOSED")
            @NotNull(message = "chat_status_required")
            String status
    ) {}
}
