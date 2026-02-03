package org.refit.refitbackend.global.config;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.JwtAuthFilter;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Bean
    public List<String> allowUrls() {
        List<String> allow = new ArrayList<>();
        allow.add("/");
        allow.add("/actuator/**");
        allow.add("/api/actuator/**");
        allow.add("/dev/**");
        allow.add("/api/api/**");
        allow.add("/api/ws/**");
        allow.add("/ws/**");

        List<String> apiPrefixes = List.of("/v1", "/api/v1");
        List<String> publicPaths = List.of(
                "/auth/oauth/**",
                "/auth/signup",
                "/auth/tokens",
                "/auth/restore",
                "/auth/dev/**",
                "/dev/**",
                "/skills",
                "/jobs",
                "/career-levels",
                "/users",
                "/experts",
                "/experts/**",
                "/email-verifications/public",
                "/email-domains"
        );

        for (String prefix : apiPrefixes) {
            for (String path : publicPaths) {
                allow.add(prefix + path);
            }
        }

        return allow;
    }

    public static final String[] SwaggerPatterns = {
            "/swagger-ui/**",
            "/api/swagger-ui/**",
            "/swagger-resources/**",
            "/api/swagger-resources/**",
            "/v3/api-docs/**",
            "/api/v3/api-docs/**",
            "/v3/api-docs/swagger-config",
            "/api/v3/api-docs/swagger-config"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource, List<String> allowUrls) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, userRepository, allowUrls);
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("Swagger");
        entryPoint.afterPropertiesSet();

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SwaggerPatterns).authenticated()
                        .requestMatchers(allowUrls.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint));

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
