package org.refit.refitbackend.domain.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationPushRequestedEvent;
import org.refit.refitbackend.domain.notification.outbox.service.NotificationOutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.notification.async.enabled"}, havingValue = "true")
public class NotificationPushEventPublisher {

    private final NotificationOutboxService notificationOutboxService;

    public void publish(NotificationPushRequestedEvent event) {
        notificationOutboxService.appendNotificationPushRequested(event);
        log.debug("Notification push outbox appended. receiverId={}, notificationId={}, type={}",
                event.receiverId(), event.notificationId(), event.type());
    }
}
