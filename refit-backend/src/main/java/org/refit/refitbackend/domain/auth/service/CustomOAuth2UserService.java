package org.refit.refitbackend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.dto.Oauth2.OAuth2KakaoUserInfoDto;
import org.refit.refitbackend.domain.auth.dto.Oauth2.OAuth2TokenInfoDto;
import org.refit.refitbackend.domain.auth.entity.RefreshToken;
import org.refit.refitbackend.domain.auth.entity.RefreshTokenStatus;
import org.refit.refitbackend.domain.auth.repository.RefreshTokenRepository;
import org.refit.refitbackend.domain.user.entity.OAuthProvider;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.util.JwtUtil;
import org.refit.refitbackend.global.util.KakaoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoUtil kakaoUtil;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 카카오 로그인/회원가입 확인
     * - 기존 회원: 로그인 성공
     * - 신규 회원: OAuth 정보 반환 (프론트에서 회원가입 폼에 활용)
     */
    @Transactional
    public AuthRes.OAuthLoginResponse kakaoLogin(AuthReq.KakaoLoginRequest request) {

        OAuth2TokenInfoDto tokenInfo = kakaoUtil.requestToken(request.code());
        OAuth2KakaoUserInfoDto kakaoUserInfo =
                kakaoUtil.requestUserInfo(tokenInfo.getAccessToken());

        return userRepository
                .findByOauthProviderAndOauthId(OAuthProvider.KAKAO, kakaoUserInfo.getId())
                .map(this::loginSuccess)
                .orElseGet(() -> signupRequired(kakaoUserInfo));
    }

    @Transactional
    public AuthRes.OAuthLoginResponse kakaoLoginWithRedirect(AuthReq.KakaoLoginRequest request, String redirectUri) {
        OAuth2TokenInfoDto tokenInfo = kakaoUtil.requestToken(request.code(), redirectUri);
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
        AuthRes.TokenDto tokens = issueTokensAndPersist(user);
        return new AuthRes.LoginSuccess(
                user.getId(),
                user.getUserType().name(),
                tokens.accessToken(),
                tokens.refreshToken()
        );
    }


    /* =======================
     * 로그인 성공
     * ======================= */
    private AuthRes.OAuthLoginResponse loginSuccess(User user) {
        AuthRes.TokenDto tokens = issueTokensAndPersist(user);
        return AuthRes.OAuthLoginResponse.builder()
                .status("LOGIN_SUCCESS")
                .loginSuccess(
                        new AuthRes.LoginSuccess(
                                user.getId(),
                                user.getUserType().name(),
                                tokens.accessToken(),
                                tokens.refreshToken()
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

    /* =======================
     * RTR 재발급
     * ======================= */
    @Transactional
    public AuthRes.TokenDto refreshTokens(String refreshToken) {
        validateRefreshToken(refreshToken);
        RefreshToken stored = getActiveRefreshToken(refreshToken);
        validateStoredRefreshToken(stored, refreshToken);
        return issueTokensAndPersist(stored.getUser());
    }

    @Transactional
    public AuthRes.TokenDto issueDevToken(AuthReq.DevTokenRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        return issueTokensAndPersist(user);
    }

    private AuthRes.TokenDto issueTokensAndPersist(User user) {
        String accessToken = jwtUtil.createAccessToken(user.getId());
        String refreshToken = jwtUtil.createRefreshToken(user.getId());
        Date expiresAt = jwtUtil.getExpiration(refreshToken);
        LocalDateTime expiresAtLocal = LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneId.systemDefault());

        refreshTokenRepository.updateStatusByUserAndStatus(
                user,
                RefreshTokenStatus.ACTIVE,
                RefreshTokenStatus.REVOKED
        );

        RefreshToken entity = RefreshToken.create(
                user,
                refreshToken,
                "unknown",
                "unknown",
                expiresAtLocal
        );
        refreshTokenRepository.save(entity);

        return new AuthRes.TokenDto(accessToken, refreshToken);
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefresh(refreshToken)) {
            throw new CustomException(ExceptionType.AUTH_INVALID_TOKEN);
        }
        if (jwtUtil.isExpired(refreshToken)) {
            refreshTokenRepository.updateStatusByToken(refreshToken, RefreshTokenStatus.EXPIRED);
            throw new CustomException(ExceptionType.AUTH_TOKEN_EXPIRED);
        }
    }

    private RefreshToken getActiveRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByTokenAndStatus(refreshToken, RefreshTokenStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ExceptionType.AUTH_INVALID_TOKEN));
    }

    private void validateStoredRefreshToken(RefreshToken stored, String refreshToken) {
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.updateStatusByToken(refreshToken, RefreshTokenStatus.EXPIRED);
            throw new CustomException(ExceptionType.AUTH_TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        if (!stored.getUser().getId().equals(userId)) {
            refreshTokenRepository.updateStatusByToken(refreshToken, RefreshTokenStatus.REVOKED);
            throw new CustomException(ExceptionType.AUTH_INVALID_TOKEN);
        }
    }


    private void validateAuthCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ExceptionType.INVALID_AUTH_CODE);
        }
    }
}
