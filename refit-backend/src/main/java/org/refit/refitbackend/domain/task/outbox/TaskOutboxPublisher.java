package org.refit.refitbackend.domain.task.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.task.kafka.event.MentorEmbeddingRefreshRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxMessage;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxStatus;
import org.refit.refitbackend.domain.task.outbox.repository.TaskOutboxRepository;
import org.refit.refitbackend.domain.task.outbox.service.TaskOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.task.outbox.enabled"}, havingValue = "true")
public class TaskOutboxPublisher {

    private final TaskOutboxRepository taskOutboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.task.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.task.outbox.retry-delay-ms:5000}")
    private long retryDelayMs;

    @Scheduled(fixedDelayString = "${app.task.outbox.publish-fixed-delay-ms:1000}")
    public void publishPendingMessages() {
        List<TaskOutboxMessage> batch = taskOutboxRepository.findPublishableBatch(
                TaskOutboxStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (TaskOutboxMessage message : batch) {
            publish(message.getId());
        }
    }

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
