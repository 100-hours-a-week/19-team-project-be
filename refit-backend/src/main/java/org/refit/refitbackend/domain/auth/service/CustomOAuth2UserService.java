package org.refit.refitbackend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.dto.Oauth2.OAuth2KakaoUserInfoDto;
import org.refit.refitbackend.domain.auth.dto.Oauth2.OAuth2TokenInfoDto;
import org.refit.refitbackend.domain.user.entity.OAuthProvider;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.util.KakaoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2UserService {

    private final UserRepository userRepository;
    private final KakaoUtil kakaoUtil;
    private final AuthService authService;

    /**
     * 카카오 로그인/회원가입 확인
     * - 기존 회원: 로그인 성공
     * - 신규 회원: OAuth 정보 반환 (프론트에서 회원가입 폼에 활용)
     */
    public AuthRes.OAuthLoginResponse kakaoLogin(AuthReq.KakaoLoginRequest request) {

        OAuth2TokenInfoDto tokenInfo = kakaoUtil.requestToken(request.code());
        OAuth2KakaoUserInfoDto kakaoUserInfo =
                kakaoUtil.requestUserInfo(tokenInfo.getAccessToken());

        return userRepository
                .findByOauthProviderAndOauthId(OAuthProvider.KAKAO, kakaoUserInfo.getId())
                .map(this::loginSuccess)
                .orElseGet(() -> signupRequired(kakaoUserInfo));
    }


    /* =======================
     * 회원가입
     * ======================= */
    @Transactional
    public AuthRes.LoginSuccess signup(AuthReq.SignUp request) {
        User user = authService.signup(request);
        return new AuthRes.LoginSuccess(
                user.getId(),
                user.getUserType().name(),
                null,
                null
        );
    }


    /* =======================
     * 로그인 성공
     * ======================= */
    private AuthRes.OAuthLoginResponse loginSuccess(User user) {
        return AuthRes.OAuthLoginResponse.builder()
                .status("LOGIN_SUCCESS")
                .loginSuccess(
                        new AuthRes.LoginSuccess(
                                user.getId(),
                                user.getUserType().name(),
                                null, // TODO accessToken
                                null  // TODO refreshToken
                        )
                )
                .build();
    }


    /* =======================
     * 회원가입 필요
     * ======================= */
    private AuthRes.OAuthLoginResponse signupRequired(OAuth2KakaoUserInfoDto kakaoUserInfo) {
        return AuthRes.OAuthLoginResponse.builder()
                .status("SIGNUP_REQUIRED")
                .signupRequired(
                        new AuthRes.SignupRequired(
                                OAuthProvider.KAKAO.name(),
                                kakaoUserInfo.getId(),
                                kakaoUserInfo.getEmail(),
                                kakaoUserInfo.getNickname(),
                                kakaoUserInfo.getProfileImageUrl()
                        )
                )
                .build();
    }


    private void validateAuthCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ExceptionType.INVALID_AUTH_CODE);
        }
    }
}