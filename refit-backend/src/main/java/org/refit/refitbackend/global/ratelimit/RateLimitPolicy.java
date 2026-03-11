package org.refit.refitbackend.global.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RateLimitPolicy {

    private final List<RateLimitRule> rules = List.of(
            // Public endpoints - IP based only (loose for shared Wi-Fi)
            new RateLimitRule("auth_post", HttpMethod.POST, "/api/v1/auth/**", 100, Duration.ofMinutes(1), RateLimitTarget.IP),
            new RateLimitRule("auth_oauth_post", HttpMethod.POST, "/api/v1/auth/oauth/**", 100, Duration.ofMinutes(1), RateLimitTarget.IP),
            new RateLimitRule("auth_restore_post", HttpMethod.POST, "/api/v1/auth/restore", 100, Duration.ofMinutes(1), RateLimitTarget.IP),
            new RateLimitRule("email_public_post", HttpMethod.POST, "/api/v1/email-verifications/public", 100, Duration.ofMinutes(1), RateLimitTarget.IP),
            new RateLimitRule("email_public_patch", HttpMethod.PATCH, "/api/v1/email-verifications/public", 100, Duration.ofMinutes(1), RateLimitTarget.IP),

            // Authenticated endpoints - user based
            new RateLimitRule("email_post", HttpMethod.POST, "/api/v1/email-verifications", 5, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("email_patch", HttpMethod.PATCH, "/api/v1/email-verifications", 5, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("presigned_post_root", HttpMethod.POST, "/api/v1/uploads/presigned-url", 20, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("presigned_post_all", HttpMethod.POST, "/api/v1/uploads/presigned-url/**", 20, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("presigned_get_root", HttpMethod.GET, "/api/v1/uploads/presigned-url", 60, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("presigned_get_all", HttpMethod.GET, "/api/v1/uploads/presigned-url/**", 60, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("chat_create", HttpMethod.POST, "/api/v1/chats", 20, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("chat_read", HttpMethod.PATCH, "/api/v1/chats/messages/read", 60, Duration.ofMinutes(1), RateLimitTarget.USER),

            // Agent endpoints - strict for expensive AI streaming
            new RateLimitRule("agent_reply_burst", HttpMethod.POST, "/api/v3/agent/reply", 3, Duration.ofSeconds(10), RateLimitTarget.USER),
            new RateLimitRule("agent_reply_minute", HttpMethod.POST, "/api/v3/agent/reply", 10, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("agent_sessions_create", HttpMethod.POST, "/api/v3/agent/sessions", 20, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("agent_sessions_list", HttpMethod.GET, "/api/v3/agent/sessions", 30, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("agent_sessions_get", HttpMethod.GET, "/api/v3/agent/sessions/*", 60, Duration.ofMinutes(1), RateLimitTarget.USER),
            new RateLimitRule("agent_messages_get", HttpMethod.GET, "/api/v3/agent/sessions/*/messages", 60, Duration.ofMinutes(1), RateLimitTarget.USER)
    );

    public List<RateLimitRule> rules() {
        return rules;
    }
}
