package org.refit.refitbackend.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.refit.refitbackend.domain.auth.dto.Oauth2.OAuth2KakaoUserInfoDto;
import org.refit.refitbackend.domain.auth.dto.Oauth2.OAuth2TokenInfoDto;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoUtil {

    private final RestTemplate restTemplate;  // RestTemplateConfig가 아니라 RestTemplate 직접 주입
    private final OAuth2ProviderProperties oAuthProvider;
    private final OAuth2RegistrationProperties oAuthRegistration;

    private HttpHeaders defaultHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    public OAuth2TokenInfoDto requestToken(String authCode) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", oAuthRegistration.kakao().clientId());
        params.add("redirect_uri", oAuthRegistration.kakao().redirectUri());
        params.add("code", authCode);
        params.add("client_secret", oAuthRegistration.kakao().clientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, defaultHeader());

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    oAuthProvider.kakao().tokenUri(),
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            return OAuth2TokenInfoDto.builder()
                    .accessToken(body.get("access_token").toString())
                    .refreshToken(body.get("refresh_token") != null ? body.get("refresh_token").toString() : null)
                    .build();
        } catch (HttpStatusCodeException e) {
            log.error("kakao token request failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                throw new CustomException(ExceptionType.INVALID_AUTH_CODE);
            }
            throw new CustomException(ExceptionType.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("kakao token request failed: {}", e.getMessage(), e);
            throw new CustomException(ExceptionType.SERVICE_UNAVAILABLE);
        }
    }

    public OAuth2KakaoUserInfoDto requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    oAuthProvider.kakao().userInfoUri(),
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = kakaoAccount.get("email") != null
                    ? kakaoAccount.get("email").toString()
                    : null;

            String profileImageUrl = profile.get("profile_image_url") != null
                    ? profile.get("profile_image_url").toString()
                    : null;

            return OAuth2KakaoUserInfoDto.builder()
                    .id(body.get("id").toString())
                    .email(email)
                    .nickname(profile.get("nickname").toString())
                    .profileImageUrl(profileImageUrl)
                    .build();

        } catch (HttpStatusCodeException e) {
            log.error("kakao user info request failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                throw new CustomException(ExceptionType.AUTH_INVALID_TOKEN);
            }
            throw new CustomException(ExceptionType.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("kakao user info request failed: {}", e.getMessage(), e);
            throw new CustomException(ExceptionType.SERVICE_UNAVAILABLE);
        }
    }
}
