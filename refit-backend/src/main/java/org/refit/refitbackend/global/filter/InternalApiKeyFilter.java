package org.refit.refitbackend.global.filter;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${internal.api-key:}")
    private String internalApiKey;

    @Value("${internal.api-key-header:X-Internal-Api-Key}")
    private String internalApiKeyHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(internalApiKeyHeader);
        if (internalApiKey == null || internalApiKey.isBlank()) {
            writeUnauthorized(response);
            return;
        }
        if (provided == null || !internalApiKey.equals(provided)) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(ExceptionType.AUTH_UNAUTHORIZED.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(
                ExceptionType.AUTH_UNAUTHORIZED.getCode(),
                ExceptionType.AUTH_UNAUTHORIZED.getMessage()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
