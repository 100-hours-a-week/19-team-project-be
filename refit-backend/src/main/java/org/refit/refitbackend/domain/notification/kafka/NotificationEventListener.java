package org.refit.refitbackend.domain.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationRequestedEvent;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationPushRequestedEvent;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.notification.async.enabled"}, havingValue = "true")
public class NotificationEventListener {

    private static final String CHAT_MESSAGE_RECEIVED = "CHAT_MESSAGE_RECEIVED";

    private final NotificationService notificationService;
    private final Optional<NotificationPushEventPublisher> notificationPushEventPublisher;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-requested:notification.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    public void onNotificationRequested(NotificationRequestedEvent event) {
        try {
            if (event == null || event.type() == null) {
                return;
            }

            if (CHAT_MESSAGE_RECEIVED.equals(event.type())) {
                handleChatMessageReceived(event);
            }
        } catch (Exception e) {
            log.warn("Notification request consume failed. type={}, senderId={}, receiverId={}, chatId={}",
                    event != null ? event.type() : null,
                    event != null ? event.senderId() : null,
                    event != null ? event.receiverId() : null,
                    event != null ? event.chatId() : null,
                    e);
            throw e;
        }
    }

    private void handleChatMessageReceived(NotificationRequestedEvent event) {
        if (event.senderId() == null || event.receiverId() == null || event.chatId() == null) {
            return;
        }

        // High-traffic chat path: skip notification DB write and send push only.
        // This keeps notification pipeline asynchronous while reducing DB pressure.
        String title = "새 메시지가 도착했어요";
        String preview = event.content() == null ? "" : event.content().trim();
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "...";
        }
        String content = preview.isBlank() ? "새 채팅 메시지가 도착했습니다." : preview;

        if (notificationPushEventPublisher.isPresent()) {
            notificationPushEventPublisher.get().publish(new NotificationPushRequestedEvent(
                    event.receiverId(),
                    null,
                    CHAT_MESSAGE_RECEIVED,
                    title,
                    content
            ));
            return;
        }

        notificationService.sendPushOnly(event.receiverId(), title, content, CHAT_MESSAGE_RECEIVED);
    }
}
