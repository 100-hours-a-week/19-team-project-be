package org.refit.refitbackend.domain.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "인증(Auth) 응답 DTO")
public record AuthRes() {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Builder
    @Schema(description = "OAuth 로그인 응답")
    public record OAuthLoginResponse(

            @Schema(description = "응답 상태", example = "LOGIN_SUCCESS | SIGNUP_REQUIRED | ACCOUNT_CHOICE_REQUIRED")
            String status,

            @Schema(description = "로그인 성공 시 데이터")
            LoginSuccess loginSuccess,

            @Schema(description = "회원가입 필요 시 데이터")
            SignupRequired signupRequired,

            @Schema(description = "복구 필요 시 데이터")
            RestoreRequired restoreRequired
    ) {}

    /* =======================
     * 로그인 성공 응답 Data
     * ======================= */
    @Schema(description = "로그인 성공 데이터")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LoginSuccess(

            @Schema(example = "1")
            Long userId,

            @Schema(example = "JOB_SEEKER")
            String userType,

            @Schema(description = "Access Token")
            String accessToken,

            @Schema(description = "Refresh Token")
            String refreshToken
    ) {}

    /* =======================
     * 회원가입 필요 응답 Data
     * ======================= */
    @Schema(description = "회원가입 필요 데이터")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SignupRequired(

            String oauthProvider,
            String oauthId,
            String email,
            String nickname
    ) {}

    /* =======================
     * 복구 필요 응답 Data
     * ======================= */
    @Schema(description = "복구 필요 데이터")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RestoreRequired(
            String oauthProvider,
            String oauthId,
            String email,
            String nickname,
            boolean emailConflict,
            boolean nicknameConflict
    ) {}

    /* =======================
     * 토큰 응답 Data
     * ======================= */
    @Schema(description = "토큰 AT, RT")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TokenDto(
            String accessToken,
            String refreshToken
    ){}

}
