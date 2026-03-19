package org.refit.refitbackend.domain.notification.realtime;

public record RedisNotificationRealtimeEvent(
        Long userId,
        String notificationType,
        Long notificationId,
        long unreadCount
) {
}
