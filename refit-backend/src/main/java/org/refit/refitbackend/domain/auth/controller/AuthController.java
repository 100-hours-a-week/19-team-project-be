package org.refit.refitbackend.domain.auth.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.auth.service.CustomOAuth2UserService;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.auth.AuthSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@Tag(name = "Auth", description = "회원가입 / 인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2RegistrationProperties registrationProperties;
    private final OAuth2ProviderProperties providerProperties;

    @AuthSwaggerSpec.Signup
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthRes.LoginSuccess>> signup(@Valid @RequestBody AuthReq.SignUp request) {
        return ResponseUtil.created("success", oAuth2UserService.signup(request));
    }

    @AuthSwaggerSpec.Restore
    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<AuthRes.LoginSuccess>> restore(@Valid @RequestBody AuthReq.Restore request) {
        return ResponseUtil.ok("restore_success", oAuth2UserService.restore(request));
    }

    /* =======================
     * 카카오 로그인
     * ======================= */
    @AuthSwaggerSpec.KakaoLogin
    @PostMapping("/oauth/kakao/login")
    public ResponseEntity<ApiResponse<AuthRes.OAuthLoginResponse>> kakaoLogin(
            @Valid @RequestBody AuthReq.KakaoLoginRequest request
    ) {
        log.info("[KAKAO][LOGIN] code={}", request.code());

        return ResponseUtil.ok("login_result", oAuth2UserService.kakaoLogin(request));
    }

    @PostMapping("/oauth/kakao/login/local")
    @AuthSwaggerSpec.KakaoLoginLocal
    public ResponseEntity<ApiResponse<AuthRes.OAuthLoginResponse>> kakaoLoginLocal(
            @Valid @RequestBody AuthReq.KakaoLoginRequest request
    ) {
        log.info("[KAKAO][LOGIN][LOCAL] code={}", request.code());

        return ResponseUtil.ok("login_result", oAuth2UserService.kakaoLoginWithRedirect(
                request,
                registrationProperties.kakao().redirectUriLocal()
        ));
    }

    @AuthSwaggerSpec.RefreshTokens
    @SecurityRequirement(name = "refreshToken")
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<AuthRes.TokenDto>> refreshTokens(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshHeader,
            @RequestBody(required = false) AuthReq.RefreshTokenRequest request
    ) {
        String refreshToken = request != null ? request.refreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = refreshHeader;
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseUtil.error(ExceptionType.AUTH_INVALID_REQUEST);
        }

        return ResponseUtil.created("token_refreshed", oAuth2UserService.refreshTokens(refreshToken));
    }

    @AuthSwaggerSpec.Logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        oAuth2UserService.logout(principal.getUserId());
        return ResponseUtil.ok("logout_success", null);
    }

    @AuthSwaggerSpec.KakaoAuthorize
    @GetMapping("/oauth/kakao/authorize")
    public ResponseEntity<Void> kakaoAuthorize() {

        log.info("[KAKAO][AUTHORIZE] authorizationUri={}", providerProperties.kakao().authorizationUri());
        log.info("[KAKAO][AUTHORIZE] clientId={}", registrationProperties.kakao().clientId());
        log.info("[KAKAO][AUTHORIZE] redirectUri={}", registrationProperties.kakao().redirectUri());

        String kakaoAuthUrl = String.format("%s?client_id=%s&redirect_uri=%s&response_type=code",
                providerProperties.kakao().authorizationUri(),
                registrationProperties.kakao().clientId(),
                registrationProperties.kakao().redirectUri());

        log.info("[KAKAO][AUTHORIZE] redirectTo={}", kakaoAuthUrl);

        return ResponseEntity.status(302)
                .header("Location", kakaoAuthUrl)
                .build();
    }

    @AuthSwaggerSpec.KakaoAuthorizeLocal
    @GetMapping("/oauth/kakao/authorize/local")
    public ResponseEntity<Void> kakaoAuthorizeLocal() {

        log.info("[KAKAO][AUTHORIZE][LOCAL] authorizationUri={}", providerProperties.kakao().authorizationUri());
        log.info("[KAKAO][AUTHORIZE][LOCAL] clientId={}", registrationProperties.kakao().clientId());
        log.info("[KAKAO][AUTHORIZE][LOCAL] redirectUri={}", registrationProperties.kakao().redirectUriLocal());

        String kakaoAuthUrl = String.format("%s?client_id=%s&redirect_uri=%s&response_type=code",
                providerProperties.kakao().authorizationUri(),
                registrationProperties.kakao().clientId(),
                registrationProperties.kakao().redirectUriLocal());

        log.info("[KAKAO][AUTHORIZE][LOCAL] redirectTo={}", kakaoAuthUrl);

        return ResponseEntity.status(302)
                .header("Location", kakaoAuthUrl)
                .build();
    }
}
