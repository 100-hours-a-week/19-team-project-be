package org.refit.refitbackend.global.ratelimit;

import org.springframework.http.HttpMethod;

import java.time.Duration;

public record RateLimitRule(
        String name,
        HttpMethod method,
        String pattern,
        int limit,
        Duration window,
        RateLimitTarget target
) {
}
