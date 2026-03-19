package org.refit.refitbackend.domain.chat.realtime;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
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
            JsonNode root = objectMapper.readTree(body);
            JsonNode chatIdNode = root.get("chatId");
            if (chatIdNode == null || !chatIdNode.canConvertToLong()) {
                throw new IllegalArgumentException("chatId missing in redis chat payload");
            }

            JsonNode payloadNode = root.get("payload");
            if (payloadNode == null || payloadNode.isNull()) {
                throw new IllegalArgumentException("payload missing in redis chat payload");
            }

            messagingTemplate.convertAndSend("/queue/chat." + chatIdNode.longValue(), payloadNode);
        } catch (Exception e) {
            log.error("Redis chat subscribe handling failed. body={}", new String(message.getBody(), StandardCharsets.UTF_8), e);
        }
    }
}
