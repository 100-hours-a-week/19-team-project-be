package org.refit.refitbackend.domain.notification.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxMessage;
import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxStatus;
import org.refit.refitbackend.domain.notification.outbox.repository.NotificationOutboxRepository;
import org.refit.refitbackend.domain.notification.outbox.service.NotificationOutboxPublishService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
    private final NotificationOutboxPublishService notificationOutboxPublishService;

    @Value("${app.notification.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notification.outbox.publish-fixed-delay-ms:1000}")
    public void publishPendingMessages() {
        List<NotificationOutboxMessage> batch = notificationOutboxRepository.findPublishableBatch(
                NotificationOutboxStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (NotificationOutboxMessage message : batch) {
            notificationOutboxPublishService.publish(message.getId());
        }
    }
}
