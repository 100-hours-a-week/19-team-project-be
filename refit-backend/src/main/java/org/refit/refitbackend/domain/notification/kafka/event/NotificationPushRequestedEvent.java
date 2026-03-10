package org.refit.refitbackend.domain.notification.kafka.event;

public record NotificationPushRequestedEvent(
        Long receiverId,
        Long notificationId,
        String type,
        String title,
        String content
) {
}
