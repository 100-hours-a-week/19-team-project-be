package org.refit.refitbackend.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.global.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            authenticateUser(accessor, token);
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization header");
        }

        return authHeader.substring(7);
    }

    private void authenticateUser(StompHeaderAccessor accessor, String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            ));

            log.info("WebSocket 인증 성공 - userId: {}", userId);

        } catch (Exception e) {
            log.error("WebSocket 인증 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }
}