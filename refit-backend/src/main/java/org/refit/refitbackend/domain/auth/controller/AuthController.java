package org.refit.refitbackend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.service.CustomOAuth2UserService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Auth", description = "회원가입 / 인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2RegistrationProperties registrationProperties;
    private final OAuth2ProviderProperties providerProperties;

    @Operation(summary = "회원가입", description = "OAuth 정보 + 추가 정보로 회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthRes.LoginSuccess>> signup(@Valid @RequestBody AuthReq.SignUp request) {
        return ResponseUtil.ok("success", oAuth2UserService.signup(request));
    }

    /* =======================
     * 카카오 로그인
     * ======================= */
    @Operation(
            summary = "카카오 로그인",
            description = """
                카카오 OAuth 로그인 처리
                - 기존 회원: 로그인 성공
                - 신규 회원: 회원가입 필요 데이터 반환
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = AuthRes.LoginSuccess.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 필요",
                    content = @Content(
                            schema = @Schema(implementation = AuthRes.SignupRequired.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            )
    })
    @PostMapping("/oauth/kakao/login")
    public ResponseEntity<ApiResponse<AuthRes.OAuthLoginResponse>> kakaoLogin(
            @Valid @RequestBody AuthReq.KakaoLoginRequest request
    ) {
        return ResponseUtil.ok("login_result", oAuth2UserService.kakaoLogin(request));
    }

    @Operation(summary = "카카오 로그인 페이지로 리다이렉트", description = "카카오 OAuth 인증 페이지로 리다이렉트")
    @GetMapping("/oauth/kakao/authorize")
    public ResponseEntity<Void> kakaoAuthorize() {
        String kakaoAuthUrl = String.format("%s?client_id=%s&redirect_uri=%s&response_type=code",
                providerProperties.kakao().authorizationUri(),
                registrationProperties.kakao().clientId(),
                registrationProperties.kakao().redirectUri());

        return ResponseEntity.status(302)
                .header("Location", kakaoAuthUrl)
                .build();
    }
}
