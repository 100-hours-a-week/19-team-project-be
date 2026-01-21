package org.refit.refitbackend.global.config;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.JwtAuthFilter;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public List<String> allowUrls() {
        return List.of(
                "/",
                "/actuator/**",
                "/api/actuator/**",
                "/index.html",
                "/callback.html",
                "/v1/auth/**",
                "/api/v1/auth/**",
                "/v1/jobs",
                "/api/v1/jobs",
                "/v1/skills",
                "/api/v1/skills",
                "/v1/career-levels",
                "/api/v1/career-levels",

                "/ws/**",

                "/swagger-ui/**",
                "/api/swagger-ui/**",
                "/swagger-resources/**",
                "/api/swagger-resources/**",
                "/v3/api-docs/**",
                "/api/v3/api-docs/**",
                "/v3/api-docs/swagger-config",
                "/api/v3/api-docs/swagger-config"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource, List<String> allowUrls) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, userRepository, allowUrls);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(allowUrls.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
