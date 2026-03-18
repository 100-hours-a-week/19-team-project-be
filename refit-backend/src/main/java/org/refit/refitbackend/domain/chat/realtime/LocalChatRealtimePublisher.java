package org.refit.refitbackend.domain.chat.realtime;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.realtime.redis.enabled", havingValue = "false", matchIfMissing = true)
public class LocalChatRealtimePublisher implements ChatRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Long chatId, ChatRes.MessageInfo payload) {
        messagingTemplate.convertAndSend("/queue/chat." + chatId, payload);
    }
}

