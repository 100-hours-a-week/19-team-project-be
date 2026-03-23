package org.refit.refitbackend.domain.task.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.task.kafka.event.MentorEmbeddingRefreshRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxMessage;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxStatus;
import org.refit.refitbackend.domain.task.outbox.repository.TaskOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOutboxPublishService {

    private final TaskOutboxRepository taskOutboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.task.outbox.retry-delay-ms:5000}")
    private long retryDelayMs;

    @Transactional
    public void publish(Long outboxId) {
        TaskOutboxMessage message = taskOutboxRepository.findById(outboxId).orElse(null);
        if (message == null || message.getStatus() != TaskOutboxStatus.PENDING) {
            return;
        }

        try {
            Object payload = deserializePayload(message);
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), payload).get(10, TimeUnit.SECONDS);
            message.markPublished();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = e.getClass().getSimpleName();
            }
            if (errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000);
            }
            message.markRetry(errorMessage, LocalDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(retryDelayMs)));
            log.warn("Task outbox publish failed. outboxId={}, eventType={}, topic={}, attempts={}",
                    message.getId(), message.getEventType(), message.getTopic(), message.getAttemptCount(), e);
        }
    }

    private Object deserializePayload(TaskOutboxMessage message) throws Exception {
        return switch (message.getEventType()) {
            case TaskOutboxService.EVENT_RESUME_PARSE_REQUESTED ->
                    objectMapper.readValue(message.getPayload(), ResumeParseRequestedEvent.class);
            case TaskOutboxService.EVENT_REPORT_GENERATE_REQUESTED ->
                    objectMapper.readValue(message.getPayload(), ReportGenerateRequestedEvent.class);
            case TaskOutboxService.EVENT_MENTOR_EMBEDDING_REFRESH_REQUESTED ->
                    objectMapper.readValue(message.getPayload(), MentorEmbeddingRefreshRequestedEvent.class);
            default -> throw new IllegalArgumentException("Unsupported task outbox event type: " + message.getEventType());
        };
    }
}
