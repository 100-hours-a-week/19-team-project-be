package org.refit.refitbackend.domain.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationPushRequestedEvent;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.notification.async.enabled"}, havingValue = "true")
public class NotificationPushListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-push-requested:notification.push.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    public void onPushRequested(NotificationPushRequestedEvent event) {
        if (event == null || event.receiverId() == null) {
            return;
        }

        try {
            notificationService.sendPushOnly(event.receiverId(), event.title(), event.content(), event.type());
        } catch (Exception e) {
            log.warn("Notification push consume failed. receiverId={}, notificationId={}, type={}",
                    event.receiverId(), event.notificationId(), event.type(), e);
            throw e;
        }
    }
}
