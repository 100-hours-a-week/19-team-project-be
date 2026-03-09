package org.refit.refitbackend.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.global.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.net.HttpCookie;
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
            log.debug("[WS] CONNECT headers: {}", accessor.toNativeHeaderMap());
            log.debug("[WS] session attributes: {}", accessor.getSessionAttributes());
            String token = extractToken(accessor);
            if (token != null && !token.isBlank()) {
                authenticateUser(accessor, token);
            } else {
                throw new MessageDeliveryException("Missing auth token on CONNECT");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Object sessionToken = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("access_token")
                : null;
        if (sessionToken instanceof String token && !token.isBlank()) {
            return token;
        }

        String cookieHeader = getCookieHeader(accessor);
        if (cookieHeader != null) {
            for (HttpCookie cookie : HttpCookie.parse(cookieHeader)) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void authenticateUser(StompHeaderAccessor accessor, String token) {
        if (!jwtUtil.validateToken(token)) {
            log.debug("WebSocket 인증 실패: Invalid JWT token");
            throw new MessageDeliveryException("Invalid JWT token");
        }

        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            log.debug("WebSocket 인증 실패: {}", e.getMessage());
            throw new MessageDeliveryException("Invalid JWT token: " + e.getMessage());
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));

        log.info("WebSocket 인증 성공 - userId: {}", userId);
    }

    private String getCookieHeader(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("cookie");
        if (headers != null && !headers.isEmpty()) {
            return String.join("; ", headers);
        }
        headers = accessor.getNativeHeader("Cookie");
        if (headers != null && !headers.isEmpty()) {
            return String.join("; ", headers);
        }
        return null;
    }
}
