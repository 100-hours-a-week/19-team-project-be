package org.refit.refitbackend.global.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {

    private static final long DEFAULT_TIMEOUT_MS = 30L * 60L * 1000L; // 30m

    private final SseEmitterRepository emitterRepository;

    public SseEmitter subscribe(Long userId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);

        emitterRepository.save(userId, emitterId, emitter);
        emitter.onCompletion(() -> emitterRepository.remove(userId, emitterId));
        emitter.onTimeout(() -> emitterRepository.remove(userId, emitterId));
        emitter.onError(ex -> emitterRepository.remove(userId, emitterId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .id(emitterId)
                    .data(Map.of(
                            "type", "CONNECTED",
                            "connected_at", Instant.now().toString()
                    )));
        } catch (IOException e) {
            emitterRepository.remove(userId, emitterId);
            throw new IllegalStateException("SSE connection initialize failed", e);
        }

        return emitter;
    }

    public void sendNotificationEvent(Long userId, String notificationType, Long notificationId, long unreadCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "NOTIFICATION");
        payload.put("notification_type", notificationType);
        payload.put("notification_id", notificationId);
        payload.put("unread_count", unreadCount);
        sendToUser(userId, "notification", payload);
    }

    public void sendChatEvent(Long userId, Long chatId, Long messageId, long unreadCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "CHAT_MESSAGE");
        payload.put("chat_id", chatId);
        payload.put("message_id", messageId);
        payload.put("unread_count", unreadCount);
        sendToUser(userId, "chat", payload);
    }

    public void sendToUser(Long userId, String eventName, Object payload) {
        for (SseEmitter emitter : emitterRepository.findAllByUserId(userId)) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload));
            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // no-op
                }
                log.debug("SSE send failed. userId={}, eventName={}", userId, eventName, e);
            }
        }
    }
}
