package org.refit.refitbackend.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class AgentRes {

    private AgentRes() {
    }

    public record SessionInfo(
            @Schema(description = "세션 ID", example = "ad6ebada-fcfe-4027-b2fc-ed45bf188982")
            String sessionId,
            @Schema(description = "세션 생성 시각(ISO-8601)", example = "2026-03-10T12:36:23.437389+00:00")
            String createdAt,
            @Schema(description = "세션 내 누적 메시지 수", example = "3")
            int messageCount,
            @Schema(description = "마지막 의도 분류값 (D1/D2/D3)", example = "D1", nullable = true)
            String lastIntent
    ) {
    }

    public record SessionList(
            @ArraySchema(schema = @Schema(implementation = SessionInfo.class))
            List<SessionInfo> sessions
    ) {
    }

    public record MessageInfo(
            @Schema(description = "메시지 ID", example = "101")
            Long id,
            @Schema(description = "세션 ID", example = "ad6ebada-fcfe-4027-b2fc-ed45bf188982")
            String sessionId,
            @Schema(description = "발화 주체 (USER/ASSISTANT/SYSTEM)", example = "USER")
            String role,
            @Schema(description = "메시지 내용", example = "백엔드 현직자 누구있어?")
            String content,
            @Schema(description = "추천/비추천 여부. assistant 메시지에만 의미가 있으며 true=추천, false=비추천, null=미평가", example = "true", nullable = true)
            Boolean feedback,
            @Schema(description = "생성 시각(ISO-8601)", example = "2026-03-10T21:36:23.437389")
            String createdAt
    ) {
    }

    public record MessageFeedbackInfo(
            @Schema(description = "메시지 ID", example = "101")
            Long messageId,
            @Schema(description = "추천/비추천 여부. true=추천, false=비추천, null=미평가", example = "true", nullable = true)
            Boolean feedback
    ) {
    }

    public record MessageList(
            @ArraySchema(schema = @Schema(implementation = MessageInfo.class))
            List<MessageInfo> messages
    ) {
    }
}
