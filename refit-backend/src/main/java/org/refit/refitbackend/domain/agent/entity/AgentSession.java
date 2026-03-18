package org.refit.refitbackend.domain.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(name = "agent_sessions", indexes = {
        @Index(name = "idx_agent_sessions_user_updated", columnList = "user_id, updated_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSession extends BaseEntity {

    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "last_intent", length = 20)
    private String lastIntent;

    public AgentSession(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.messageCount = 0;
    }

    public void increaseMessageCount() {
        this.messageCount += 1;
    }

    public void updateLastIntent(String intent) {
        if (intent == null || intent.isBlank()) {
            return;
        }
        this.lastIntent = intent;
    }
}
