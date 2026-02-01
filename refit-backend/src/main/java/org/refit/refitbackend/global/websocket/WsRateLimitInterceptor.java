package org.refit.refitbackend.global.websocket;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.ratelimit.LocalRateLimiter;
import org.refit.refitbackend.global.ratelimit.RateLimitResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WsRateLimitInterceptor implements ChannelInterceptor {

    private static final int MAX_PER_SECOND = 5;
    private static final Duration WINDOW = Duration.ofSeconds(1);
    private static final String CHAT_SEND_DESTINATION = "/app/chat.sendMessage";
    private static final String ERROR_DESTINATION = "/queue/errors";

    private final LocalRateLimiter rateLimiter;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SEND.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (!CHAT_SEND_DESTINATION.equals(destination)) {
            return message;
        }

        String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
        if (userId == null || userId.isBlank()) {
            return message;
        }

        RateLimitResult result = rateLimiter.check("ws:user:" + userId + ":chat_send", MAX_PER_SECOND, WINDOW);
        if (!result.allowed()) {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    ERROR_DESTINATION,
                    "rate_limit_exceeded"
            );
            return null;
        }

        return message;
    }
}
