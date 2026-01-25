package org.refit.refitbackend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.service.CustomOAuth2UserService;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.response.ErrorResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
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

    @Operation(
            summary = "회원가입",
            description = """
                OAuth 정보 + 추가 정보로 회원가입합니다.

                - JOB_SEEKER: 회사 정보 없이 가입 가능
                - EXPERT: company_email을 보낼 수 있으며, 인증 완료된 이메일이면 verified=true로 저장
                - 인증되지 않은 이메일이거나 미입력 시 verified=false로 가입됩니다.
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = AuthReq.SignUp.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "job_seeker",
                                    value = "{ \"oauth_provider\": \"KAKAO\", \"oauth_id\": \"123456\", \"email\": \"user@kakao.com\", \"nickname\": \"eden\", \"user_type\": \"JOB_SEEKER\", \"career_level_id\": 1, \"job_ids\": [1, 2], \"skills\": [{\"skill_id\": 1, \"display_order\": 1}], \"introduction\": \"백엔드 개발자입니다.\" }"
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "expert_unverified",
                                    value = "{ \"oauth_provider\": \"KAKAO\", \"oauth_id\": \"123456\", \"email\": \"user@kakao.com\", \"nickname\": \"eden\", \"user_type\": \"EXPERT\", \"career_level_id\": 1, \"job_ids\": [1], \"skills\": [{\"skill_id\": 1, \"display_order\": 1}], \"introduction\": \"백엔드 개발자입니다.\", \"company_name\": \"네이버\", \"company_email\": \"user@navercorp.com\" }"
                            )
                    }
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthRes.LoginSuccess>> signup(@Valid @RequestBody AuthReq.SignUp request) {
        return ResponseUtil.created("success", oAuth2UserService.signup(request));
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
                    description = "invalid_request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/oauth/kakao/login")
    public ResponseEntity<ApiResponse<AuthRes.OAuthLoginResponse>> kakaoLogin(
            @Valid @RequestBody AuthReq.KakaoLoginRequest request
    ) {
        log.info("[KAKAO][LOGIN] code={}", request.code());

        return ResponseUtil.ok("login_result", oAuth2UserService.kakaoLogin(request));
    }

    @Operation(summary = "토큰 재발급", description = "RTR 방식으로 AT/RT 재발급")
    @SecurityRequirement(name = "refreshToken")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "created",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "token_refreshed",
                                    value = "{ \"code\": \"CREATED\", \"message\": \"token_refreshed\", \"data\": { \"access_token\": \"string\", \"refresh_token\": \"string\" } }"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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

    @Operation(summary = "카카오 로그인 페이지로 리다이렉트", description = "카카오 OAuth 인증 페이지로 리다이렉트")
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
}
