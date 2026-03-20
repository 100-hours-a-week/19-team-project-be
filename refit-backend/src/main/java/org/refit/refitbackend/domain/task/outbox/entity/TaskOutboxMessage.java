package org.refit.refitbackend.domain.task.outbox.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_outbox", indexes = {
        @Index(name = "idx_task_outbox_status_next_attempt", columnList = "status, next_attempt_at, id"),
        @Index(name = "idx_task_outbox_published_at", columnList = "published_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskOutboxMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "message_key", length = 100)
    private String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Builder
    private TaskOutboxMessage(
            String eventType,
            String topic,
            String messageKey,
            String payload,
            TaskOutboxStatus status,
            int attemptCount,
            LocalDateTime nextAttemptAt,
            LocalDateTime publishedAt,
            String lastError
    ) {
        this.eventType = eventType;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.publishedAt = publishedAt;
        this.lastError = lastError;
    }

    public static TaskOutboxMessage pending(String eventType, String topic, String messageKey, String payload) {
        return TaskOutboxMessage.builder()
                .eventType(eventType)
                .topic(topic)
                .messageKey(messageKey)
                .payload(payload)
                .status(TaskOutboxStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();
    }

    public void markPublished() {
        this.status = TaskOutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markRetry(String errorMessage, LocalDateTime nextAttemptAt) {
        this.status = TaskOutboxStatus.PENDING;
        this.attemptCount += 1;
        this.lastError = errorMessage;
        this.nextAttemptAt = nextAttemptAt;
    }
}
