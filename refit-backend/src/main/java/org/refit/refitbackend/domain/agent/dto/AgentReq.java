package org.refit.refitbackend.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AgentReq {

    private AgentReq() {
    }

    public record ReplyRequest(
            @Schema(description = "대화 세션 ID (없으면 자동 생성)", example = "ad6ebada-fcfe-4027-b2fc-ed45bf188982")
            String sessionId,
            @NotBlank
            @Schema(description = "사용자 메시지", example = "Spring Boot, Redis, Kafka에 능숙한 현직자 추천해줘")
            String message,
            @Min(1)
            @Max(20)
            @Schema(description = "추천 멘토 수 (1~20, 기본 3)", example = "3", minimum = "1", maximum = "20")
            Integer topK
    ) {
        public int normalizedTopK() {
            return topK == null ? 3 : topK;
        }
    }
}
