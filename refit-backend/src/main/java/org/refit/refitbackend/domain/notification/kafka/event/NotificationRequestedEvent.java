package org.refit.refitbackend.domain.notification.kafka.event;

public record NotificationRequestedEvent(
        String type,
        Long senderId,
        Long receiverId,
        Long chatId,
        String content
) {
}
