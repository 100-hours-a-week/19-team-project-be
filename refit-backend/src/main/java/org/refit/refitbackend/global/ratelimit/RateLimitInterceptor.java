package org.refit.refitbackend.global.ratelimit;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitPolicy policy;
    private final LocalRateLimiter rateLimiter;
    private final RateLimitMatcher matcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = resolveUserId(request);
        if (userId == null) {
            return true;
        }

        String path = request.getRequestURI();
        for (RateLimitRule rule : policy.rules()) {
            if (rule.target() != RateLimitTarget.USER) {
                continue;
            }
            if (!matcher.matches(rule, request.getMethod(), path)) {
                continue;
            }
            String key = "rate:user:" + userId + ":" + rule.name();
            RateLimitResult result = rateLimiter.check(key, rule.limit(), rule.window());
            if (!result.allowed()) {
                writeRateLimit(response, result.retryAfterSeconds());
                return false;
            }
        }
        return true;
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void writeRateLimit(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(ExceptionType.RATE_LIMIT_EXCEEDED.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        ApiResponse<Void> body = ApiResponse.error(
                ExceptionType.RATE_LIMIT_EXCEEDED.getCode(),
                ExceptionType.RATE_LIMIT_EXCEEDED.getMessage()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
