package org.refit.refitbackend.global.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "app.metrics.db-query-count", name = "enabled", havingValue = "true")
public class DbQueryCountFilter extends OncePerRequestFilter {

    private static final String METRIC_NAME = "db.query.count";
    private static final String CASE_HEADER = "X-Search-Case";

    private final MeterRegistry meterRegistry;

    public DbQueryCountFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        QueryCountHolder.clear();
        try {
            filterChain.doFilter(request, response);
        } finally {
            QueryCount queryCount = QueryCountHolder.getGrandTotal();
            if (queryCount != null) {
                String uri = resolveUriPattern(request);
                String method = request.getMethod();
                String status = String.valueOf(response.getStatus());
                String requestCase = resolveCase(request);

                DistributionSummary.builder(METRIC_NAME)
                        .tags("uri", uri, "method", method, "status", status, "case", requestCase)
                        .register(meterRegistry)
                        .record(queryCount.getTotal());
            }
            QueryCountHolder.clear();
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/api/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
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
