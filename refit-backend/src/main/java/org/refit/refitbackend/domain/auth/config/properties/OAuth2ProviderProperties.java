package org.refit.refitbackend.domain.auth.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.oauth2.client.provider")
public record OAuth2ProviderProperties(
        KakaoProvider kakao
) {
    public record KakaoProvider(
            String authorizationUri,
            String tokenUri,
            String userInfoUri,
            String userNameAttribute
    ) {}
}