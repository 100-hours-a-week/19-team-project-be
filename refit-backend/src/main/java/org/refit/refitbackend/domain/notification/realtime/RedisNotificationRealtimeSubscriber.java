package org.refit.refitbackend.domain.notification.realtime;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.global.sse.SseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.realtime.redis.enabled", havingValue = "true")
public class RedisNotificationRealtimeSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SseService sseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(body);

            JsonNode userIdNode = root.get("userId");
            if (userIdNode == null || userIdNode.isNull()) {
                userIdNode = root.get("user_id");
            }
            if (userIdNode == null || !userIdNode.canConvertToLong()) {
                throw new IllegalArgumentException("userId missing in redis notification payload");
            }

            JsonNode notificationTypeNode = root.get("notificationType");
            if (notificationTypeNode == null || notificationTypeNode.isNull()) {
                notificationTypeNode = root.get("notification_type");
            }
            JsonNode notificationIdNode = root.get("notificationId");
            if (notificationIdNode == null || notificationIdNode.isNull()) {
                notificationIdNode = root.get("notification_id");
            }
            JsonNode unreadCountNode = root.get("unreadCount");
            if (unreadCountNode == null || unreadCountNode.isNull()) {
                unreadCountNode = root.get("unread_count");
            }

            sseService.sendNotificationEventLocal(
                    userIdNode.longValue(),
                    notificationTypeNode != null && !notificationTypeNode.isNull() ? notificationTypeNode.asText() : null,
                    notificationIdNode != null && notificationIdNode.canConvertToLong() ? notificationIdNode.longValue() : null,
                    unreadCountNode != null && unreadCountNode.canConvertToLong() ? unreadCountNode.longValue() : 0L
            );
        } catch (Exception e) {
            log.error("Redis notification subscribe handling failed. body={}",
                    new String(message.getBody(), StandardCharsets.UTF_8), e);
        }
    }
}
