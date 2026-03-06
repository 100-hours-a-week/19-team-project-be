package org.refit.refitbackend.domain.chat.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.realtime.redis.enabled", havingValue = "true")
public class RedisChatRealtimeSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            RedisChatRealtimeEvent event = objectMapper.readValue(body, RedisChatRealtimeEvent.class);
            messagingTemplate.convertAndSend("/queue/chat." + event.chatId(), event.payload());
        } catch (Exception e) {
            log.error("Redis chat subscribe handling failed", e);
        }
    }
}

