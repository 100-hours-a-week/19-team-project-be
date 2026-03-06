package org.refit.refitbackend.domain.chat.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.realtime.redis.enabled", havingValue = "true")
public class RedisChatRealtimePublisher implements ChatRealtimePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.chat.realtime.redis.channel:chat.message.broadcast}")
    private String channel;

    @Override
    public void publish(Long chatId, ChatRes.MessageInfo payload) {
        try {
            String body = objectMapper.writeValueAsString(new RedisChatRealtimeEvent(chatId, payload));
            stringRedisTemplate.convertAndSend(channel, body);
        } catch (JsonProcessingException e) {
            log.error("Redis chat publish serialization failed. chatId={}", chatId, e);
        } catch (Exception e) {
            log.error("Redis chat publish failed. chatId={}", chatId, e);
        }
    }
}

