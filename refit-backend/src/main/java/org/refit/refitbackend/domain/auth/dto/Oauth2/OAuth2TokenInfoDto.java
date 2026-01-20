package org.refit.refitbackend.domain.auth.dto.Oauth2;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth2TokenInfoDto {

    private String accessToken;
    private String refreshToken;

    @Builder
    public OAuth2TokenInfoDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}