package org.refit.refitbackend.global.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRepository {

    private final Map<Long, Map<String, SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public void save(Long userId, String emitterId, SseEmitter emitter) {
        emittersByUserId
                .computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);
    }

    public List<SseEmitter> findAllByUserId(Long userId) {
        Map<String, SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return List.of();
        }
        return List.copyOf(emitters.values());
    }

    public void remove(Long userId, String emitterId) {
        Map<String, SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitterId);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
