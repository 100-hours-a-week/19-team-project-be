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

import java.net.HttpCookie;
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
            log.info("[WS] CONNECT headers: {}", accessor.toNativeHeaderMap());
            String token = extractToken(accessor);
            authenticateUser(accessor, token);
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

        throw new IllegalArgumentException("Missing authentication token");
    }

    private void authenticateUser(StompHeaderAccessor accessor, String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new IllegalArgumentException("Invalid JWT token");
            }
            Long userId = jwtUtil.getUserId(token);

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            ));

            log.info("WebSocket 인증 성공 - userId: {}", userId);

        } catch (Exception e) {
            log.error("WebSocket 인증 실패", e);
        }
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
