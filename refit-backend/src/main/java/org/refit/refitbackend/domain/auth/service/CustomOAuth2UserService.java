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
import org.refit.refitbackend.domain.user.entity.enums.UserStatus;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.util.JwtUtil;
import org.refit.refitbackend.global.util.KakaoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisRefreshTokenStore redisRefreshTokenStore;
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
                .map(user -> user.isDeleted()
                        ? restoreRequired(kakaoUserInfo)
                        : loginSuccess(user))
                .orElseGet(() -> signupRequired(kakaoUserInfo));
    }

    @Transactional
    public AuthRes.OAuthLoginResponse kakaoLoginWithRedirect(AuthReq.KakaoLoginRequest request, String redirectUri) {
        OAuth2TokenInfoDto tokenInfo = kakaoUtil.requestToken(request.code(), redirectUri);
        OAuth2KakaoUserInfoDto kakaoUserInfo =
                kakaoUtil.requestUserInfo(tokenInfo.getAccessToken());

        return userRepository
                .findByOauthProviderAndOauthId(OAuthProvider.KAKAO, kakaoUserInfo.getId())
                .map(user -> user.isDeleted()
                        ? restoreRequired(kakaoUserInfo)
                        : loginSuccess(user))
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

    @Transactional
    public AuthRes.LoginSuccess restore(AuthReq.Restore request) {
        User user = authService.restore(request);
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
                                kakaoUserInfo.getNickname()
                        )
                )
                .build();
    }

    private AuthRes.OAuthLoginResponse restoreRequired(OAuth2KakaoUserInfoDto kakaoUserInfo) {
        String email = kakaoUserInfo.getEmail();
        String nickname = kakaoUserInfo.getNickname();
        boolean emailConflict = email == null || email.isBlank()
                || userRepository.existsByEmailAndStatus(email, UserStatus.ACTIVE);
        boolean nicknameConflict = nickname == null || nickname.isBlank()
                || userRepository.existsByNicknameAndStatus(nickname, UserStatus.ACTIVE);

        return AuthRes.OAuthLoginResponse.builder()
                .status("ACCOUNT_CHOICE_REQUIRED")
                .restoreRequired(
                        new AuthRes.RestoreRequired(
                                OAuthProvider.KAKAO.name(),
                                kakaoUserInfo.getId(),
                                email,
                                nickname,
                                emailConflict,
                                nicknameConflict
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
        RefreshToken stored = validateStoredRefreshToken(refreshToken);
        if (stored.getUser().isDeleted()) {
            revokeRefreshToken(refreshToken);
            throw new CustomException(ExceptionType.USER_DELETED);
        }
        return issueTokensAndPersist(stored.getUser());
    }

    @Transactional
    public AuthRes.TokenDto issueDevToken(AuthReq.DevTokenRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(ExceptionType.USER_DELETED);
        }
        return issueTokensAndPersist(user);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(ExceptionType.USER_DELETED);
        }

        refreshTokenRepository.updateStatusByUserAndStatus(
                user,
                RefreshTokenStatus.ACTIVE,
                RefreshTokenStatus.REVOKED
        );
        revokeAllRefreshTokens(userId);
    }

    private AuthRes.TokenDto issueTokensAndPersist(User user) {
        String accessToken = jwtUtil.createAccessToken(user.getId());
        String refreshToken = jwtUtil.createRefreshToken(user.getId());
        Date expiresAt = jwtUtil.getExpiration(refreshToken);
        Duration ttl = Duration.ofMillis(Math.max(1L, expiresAt.getTime() - System.currentTimeMillis()));

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
                java.time.LocalDateTime.now().plus(ttl)
        );
        refreshTokenRepository.save(entity);
        cacheRefreshToken(user.getId(), refreshToken, ttl);

        return new AuthRes.TokenDto(accessToken, refreshToken);
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefresh(refreshToken)) {
            throw new CustomException(ExceptionType.AUTH_INVALID_TOKEN);
        }
        if (jwtUtil.isExpired(refreshToken)) {
            refreshTokenRepository.updateStatusByToken(refreshToken, RefreshTokenStatus.EXPIRED);
            revokeRefreshToken(refreshToken);
            throw new CustomException(ExceptionType.AUTH_TOKEN_EXPIRED);
        }
    }

    private RefreshToken validateStoredRefreshToken(String refreshToken) {
        Long userId = jwtUtil.getUserId(refreshToken);
        if (isActiveInRedis(userId, refreshToken)) {
            return resolveActiveRefreshToken(refreshToken)
                    .orElseGet(() -> {
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
                        return RefreshToken.create(
                                user,
                                refreshToken,
                                "unknown",
                                "unknown",
                                java.time.LocalDateTime.now().plusSeconds(1)
                        );
                    });
        }

        RefreshToken stored = resolveActiveRefreshToken(refreshToken)
                .orElseThrow(() -> new CustomException(ExceptionType.AUTH_INVALID_TOKEN));

        if (!stored.getUser().getId().equals(userId)) {
            refreshTokenRepository.updateStatusByToken(refreshToken, RefreshTokenStatus.REVOKED);
            revokeRefreshToken(refreshToken);
            throw new CustomException(ExceptionType.AUTH_INVALID_TOKEN);
        }

        if (stored.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            refreshTokenRepository.updateStatusByToken(refreshToken, RefreshTokenStatus.EXPIRED);
            revokeRefreshToken(refreshToken);
            throw new CustomException(ExceptionType.AUTH_TOKEN_EXPIRED);
        }

        Duration ttl = Duration.between(java.time.LocalDateTime.now(), stored.getExpiresAt());
        if (!ttl.isNegative() && !ttl.isZero()) {
            cacheRefreshToken(userId, refreshToken, ttl);
        }

        return stored;
    }

    private java.util.Optional<RefreshToken> resolveActiveRefreshToken(String refreshToken) {
        List<RefreshToken> matches = refreshTokenRepository.findAllByTokenAndStatusOrderByIdDesc(
                refreshToken,
                RefreshTokenStatus.ACTIVE
        );
        if (matches.isEmpty()) {
            return java.util.Optional.empty();
        }

        RefreshToken latest = matches.getFirst();
        if (matches.size() > 1) {
            int revokedCount = refreshTokenRepository.updateStatusByTokenAndStatusExcludingId(
                    refreshToken,
                    RefreshTokenStatus.ACTIVE,
                    RefreshTokenStatus.REVOKED,
                    latest.getId()
            );
            log.warn("Duplicate active refresh tokens detected. tokenHash={}, revokedDuplicates={}",
                    Integer.toHexString(refreshToken.hashCode()),
                    revokedCount);
        }
        return java.util.Optional.of(latest);
    }

    private boolean isActiveInRedis(Long userId, String refreshToken) {
        try {
            return redisRefreshTokenStore.isActive(userId, refreshToken);
        } catch (Exception e) {
            log.warn("Redis refresh token lookup failed. Fallback to DB. userId={}", userId, e);
            return false;
        }
    }

    private void cacheRefreshToken(Long userId, String refreshToken, Duration ttl) {
        try {
            redisRefreshTokenStore.rotate(userId, refreshToken, ttl);
        } catch (Exception e) {
            log.warn("Redis refresh token cache write failed. userId={}", userId, e);
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        try {
            redisRefreshTokenStore.revoke(refreshToken);
        } catch (Exception e) {
            log.warn("Redis refresh token revoke failed", e);
        }
    }

    private void revokeAllRefreshTokens(Long userId) {
        try {
            redisRefreshTokenStore.revokeAllByUserId(userId);
        } catch (Exception e) {
            log.warn("Redis refresh token revokeAll failed. userId={}", userId, e);
        }
    }


    private void validateAuthCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ExceptionType.INVALID_AUTH_CODE);
        }
    }
}
