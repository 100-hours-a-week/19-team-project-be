package org.refit.refitbackend.domain.notification.realtime;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.realtime.redis.enabled", havingValue = "true")
public class RedisNotificationRealtimePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.notification.realtime.redis.channel:notification.event.broadcast}")
    private String channel;

    public void publish(Long userId, String notificationType, Long notificationId, long unreadCount) {
        try {
            String body = objectMapper.writeValueAsString(
                    new RedisNotificationRealtimeEvent(userId, notificationType, notificationId, unreadCount)
            );
            stringRedisTemplate.convertAndSend(channel, body);
        } catch (Exception e) {
            log.error("Redis notification publish failed. userId={}, notificationId={}", userId, notificationId, e);
        }
    }
}
