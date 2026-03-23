package org.refit.refitbackend.domain.notification.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationPushRequestedEvent;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationRequestedEvent;
import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxMessage;
import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxStatus;
import org.refit.refitbackend.domain.notification.outbox.repository.NotificationOutboxRepository;
import org.refit.refitbackend.domain.notification.outbox.service.NotificationOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {
        "app.kafka.enabled",
        "app.notification.async.enabled",
        "app.notification.outbox.enabled"
}, havingValue = "true")
public class NotificationOutboxPublisher {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.notification.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.notification.outbox.retry-delay-ms:5000}")
    private long retryDelayMs;

    @Scheduled(fixedDelayString = "${app.notification.outbox.publish-fixed-delay-ms:1000}")
    public void publishPendingMessages() {
        List<NotificationOutboxMessage> batch = notificationOutboxRepository.findPublishableBatch(
                NotificationOutboxStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (NotificationOutboxMessage message : batch) {
            publish(message.getId());
        }
    }

    @Transactional
    public void publish(Long outboxId) {
        NotificationOutboxMessage message = notificationOutboxRepository.findById(outboxId).orElse(null);
        if (message == null || message.getStatus() != NotificationOutboxStatus.PENDING) {
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
            log.warn("Notification outbox publish failed. outboxId={}, eventType={}, topic={}, attempts={}",
                    message.getId(), message.getEventType(), message.getTopic(), message.getAttemptCount(), e);
        }
    }

    private Object deserializePayload(NotificationOutboxMessage message) throws Exception {
        return switch (message.getEventType()) {
            case NotificationOutboxService.EVENT_NOTIFICATION_REQUESTED ->
                    objectMapper.readValue(message.getPayload(), NotificationRequestedEvent.class);
            case NotificationOutboxService.EVENT_NOTIFICATION_PUSH_REQUESTED ->
                    objectMapper.readValue(message.getPayload(), NotificationPushRequestedEvent.class);
            default -> throw new IllegalArgumentException("Unsupported notification outbox event type: " + message.getEventType());
        };
    }
}
