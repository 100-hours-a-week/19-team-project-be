package org.refit.refitbackend.global.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

@Component
public class ExpertSearchRequestMetricsFilter extends OncePerRequestFilter {

    private static final String METRIC_NAME = "expert.search.request";
    private static final String CASE_HEADER = "X-Search-Case";
    private static final String TARGET_URI = "/api/v1/experts";

    private final MeterRegistry meterRegistry;

    public ExpertSearchRequestMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isTarget(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            filterChain.doFilter(request, response);
        } finally {
            String uri = resolveUriPattern(request);
            String method = request.getMethod();
            String status = String.valueOf(response.getStatus());
            String requestCase = resolveCase(request);

            Timer timer = Timer.builder(METRIC_NAME)
                    .tags("uri", uri, "method", method, "status", status, "case", requestCase)
                    .publishPercentileHistogram(true)
                    .register(meterRegistry);
            sample.stop(timer);
        }
    }

    private boolean isTarget(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && TARGET_URI.equals(request.getRequestURI());
    }

    private String resolveUriPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }

    private String resolveCase(HttpServletRequest request) {
        String value = request.getHeader(CASE_HEADER);
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
