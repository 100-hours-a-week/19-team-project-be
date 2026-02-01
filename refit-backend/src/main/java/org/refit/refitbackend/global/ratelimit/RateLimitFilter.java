package org.refit.refitbackend.global.ratelimit;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitPolicy policy;
    private final LocalRateLimiter rateLimiter;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimitMatcher matcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isSwaggerPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        for (RateLimitRule rule : policy.rules()) {
            if (rule.target() != RateLimitTarget.IP) {
                continue;
            }
            if (!matcher.matches(rule, request.getMethod(), path)) {
                continue;
            }
            String ip = keyResolver.resolveClientIp(request);
            String key = "rate:ip:" + ip + ":" + rule.name();
            RateLimitResult result = rateLimiter.check(key, rule.limit(), rule.window());
            if (!result.allowed()) {
                writeRateLimit(response, result.retryAfterSeconds());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSwaggerPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/swagger-resources")
                || path.startsWith("/api/v3/api-docs");
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
