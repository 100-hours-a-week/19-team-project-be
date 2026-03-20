package org.refit.refitbackend.domain.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationRequestedEvent;
import org.refit.refitbackend.domain.notification.outbox.service.NotificationOutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.notification.async.enabled"}, havingValue = "true")
public class NotificationEventPublisher {

    private final NotificationOutboxService notificationOutboxService;

    public void publishNotificationRequested(NotificationRequestedEvent event) {
        notificationOutboxService.appendNotificationRequested(event);
        log.debug("Notification outbox appended. type={}, senderId={}, receiverId={}, chatId={}",
                event.type(), event.senderId(), event.receiverId(), event.chatId());
    }
}
