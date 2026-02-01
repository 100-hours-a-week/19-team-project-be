package org.refit.refitbackend.domain.auth.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration")
public record OAuth2RegistrationProperties(
        KakaoRegistration kakao
) {
    public record KakaoRegistration(
            String clientId,
            String clientSecret,
            String redirectUri,
            String redirectUriLocal,
            List<String> scope
    ) {}
}