package org.refit.refitbackend.domain.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatReq {

    @Schema(description = "채팅방 생성 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateRoom(
            @Schema(description = "수신자(현직자) ID", example = "2")
            @NotNull(message = "수신자 ID가 필요합니다.")
            Long receiverId,

            @Schema(description = "이력서 ID (선택)", example = "1")
            Long resumeId,

            @Schema(description = "공고 URL (선택)", example = "https://example.com/job/123")
            @Size(max = 500, message = "공고 링크가 너무 깁니다.")
            String jobPostUrl,

            @Schema(description = "채팅 요청 유형", example = "FEEDBACK / COFFEE_CHAT")
            @NotNull(message = "요청 타입이 필요합니다.")
            String requestType
    ) {}

    @Schema(description = "메시지 전송 요청 (WebSocket)")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SendMessage(
            @Schema(description = "채팅방 ID", example = "1")
            @NotNull(message = "채팅 ID가 필요합니다.")
            Long chatId,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            @NotNull(message = "메시지 내용을 입력해 주세요.")
            @Size(min = 1, max = 500, message = "메시지 내용이 너무 깁니다.")
            String content,

            @Schema(description = "메시지 타입", example = "TEXT")
            String messageType  // TEXT, FILE, SYSTEM
    ) {}

    @Schema(description = "메시지 읽음 처리 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ReadMessage(
            @Schema(description = "채팅방 ID", example = "1")
            @NotNull(message = "채팅 ID가 필요합니다.")
            Long chatId,

            @Schema(description = "읽은 메시지 ID", example = "100")
            @NotNull(message = "메시지 ID가 필요합니다.")
            Long messageId
    ) {}

    @Schema(description = "채팅방 종료 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CloseRoom(
            @Schema(description = "채팅방 상태", example = "CLOSED")
            @NotNull(message = "채팅 상태가 필요합니다.")
            String status
    ) {}
}
