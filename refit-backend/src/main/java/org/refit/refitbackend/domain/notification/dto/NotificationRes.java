package org.refit.refitbackend.domain.notification.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.refit.refitbackend.domain.notification.entity.FcmToken;
import org.refit.refitbackend.domain.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationRes {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NotificationItem(
            Long notificationId,
            String type,
            String title,
            String content,
            boolean isRead,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        public static NotificationItem from(Notification notification) {
            return new NotificationItem(
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getContent(),
                    notification.isRead(),
                    notification.getReadAt(),
                    notification.getCreatedAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NotificationListResponse(
            List<NotificationItem> notifications,
            String nextCursor,
            boolean hasMore,
            long unreadCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ReadNotificationResponse(
            Long notificationId,
            boolean isRead,
            LocalDateTime readAt
    ) {
        public static ReadNotificationResponse from(Notification notification) {
            return new ReadNotificationResponse(
                    notification.getId(),
                    notification.isRead(),
                    notification.getReadAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ReadAllNotificationsResponse(
            int updatedCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FcmTokenResponse(
            Long fcmTokenId,
            String token,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static FcmTokenResponse from(FcmToken fcmToken) {
            return new FcmTokenResponse(
                    fcmToken.getId(),
                    fcmToken.getToken(),
                    fcmToken.getCreatedAt(),
                    fcmToken.getUpdatedAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DeleteFcmTokenResponse(
            boolean deleted
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestPushResponse(
            boolean sent,
            String token,
            String title,
            int requestedCount,
            int successCount,
            int failureCount,
            boolean attempted
    ) {}
}
