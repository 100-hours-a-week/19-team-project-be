package org.refit.refitbackend.domain.auth.jwt;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.JwtUtil;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.refit.refitbackend.global.util.JwtUtil.extractBearer;


public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final List<String> allowUrls;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository, List<String> allowedUrls) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.allowUrls = allowedUrls;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();

        if (shouldSkip(path, request.getMethod(), request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        String token = extractBearer(authorization);

        if (token == null) {
            writeError(response, ExceptionType.AUTH_UNAUTHORIZED);
            return;
        }
        if (!jwtUtil.isValid(token)) {
            writeError(response, ExceptionType.AUTH_INVALID_TOKEN);
            return;
        }
        if (jwtUtil.isExpired(token)) {
            writeError(response, ExceptionType.AUTH_TOKEN_EXPIRED);
            return;
        }

        Long userId = jwtUtil.getUserId(token);

        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            writeError(response, ExceptionType.USER_NOT_FOUND);
            return;
        }

        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute("userId", userId);
        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path, String method, HttpServletRequest request) {
        if (isSwaggerPath(path)) {
            return true;
        }
        for (String pattern : allowUrls) {
            if ("/".equals(pattern)) {
                if ("/".equals(path)) return true;
                continue;
            }
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.startsWith(prefix)) return true;
                continue;
            }
            if (path.equals(pattern)) return true;
        }
        return false;
    }

    private boolean isSwaggerPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/swagger-resources")
                || path.startsWith("/api/v3/api-docs");
    }


    private void writeError(HttpServletResponse response, ExceptionType type) throws IOException {
        response.setStatus(type.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> body = ApiResponse.error(type.getCode(), type.getMessage());
        String json = objectMapper.writeValueAsString(body);
        response.getWriter().write(json);
    }
}
