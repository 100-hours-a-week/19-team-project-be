package org.refit.refitbackend.domain.auth.dto.Oauth2;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth2KakaoUserInfoDto {
    private String id;
    private String email;
    private String nickname;
    private String profileImageUrl;

    @Builder
    public OAuth2KakaoUserInfoDto(String id, String email, String nickname, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}