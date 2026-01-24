package org.refit.refitbackend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.refit.refitbackend.domain.user.entity.OAuthProvider;
import org.refit.refitbackend.domain.user.entity.enums.UserType;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthReq {

    /* =======================
     * 카카오 로그인 요청
     * ======================= */
    public static record KakaoLoginRequest(
            @NotBlank(message = "auth_code_required")
            @Schema(description = "카카오 인가 코드")
            String code
    ) {}


    /* =======================
     * 회원가입 (OAuth 정보 + 추가 정보 한번에)
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record SignUp(
            @NotNull(message = "signup_oauth_provider_invalid")
            @Schema(description = "OAuth Provider", example = "KAKAO")
            OAuthProvider oauthProvider,

            @NotBlank(message = "signup_oauth_id_empty")
            @Schema(description = "OAuth 고유 ID")
            String oauthId,

            @Email(message = "signup_email_invalid")
            @Schema(description = "이메일")
            String email,

            @NotBlank(message = "nickname_empty")
            @Size(min = 2, max = 10, message = "nickname_length_invalid")
            @Schema(description = "닉네임")
            String nickname,

            @NotNull(message = "signup_user_type_invalid")
            @Schema(description = "사용자 유형")
            UserType userType,

            @NotNull(message = "career_level_not_found")
            @Schema(description = "경력 레벨 ID")
            Long careerLevelId,

            @Schema(description = "직무 ID 목록")
            List<Long> jobIds,

            @Schema(description = "스킬 목록")
            List<SkillRequest> skills,

            @Schema(description = "자기소개")
            String introduction
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record SkillRequest(
            @Schema(description = "스킬 ID", example = "1", name = "skill_id")
            Long skillId,
            @Schema(description = "표시 순서", example = "1", name = "display_order")
            Integer displayOrder
    ) {}

    /* =======================
     * 토큰 재발급 요청 (RTR)
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record RefreshTokenRequest(
            @NotBlank(message = "refresh_token_required")
            @Schema(description = "Refresh Token")
            String refreshToken
    ) {}

    /* =======================
     * 개발용 토큰 발급 요청
     * ======================= */
    public static record DevTokenRequest(
            @NotNull(message = "user_id_required")
            @Schema(description = "User ID")
            @JsonProperty("user_id")
            Long userId
    ) {}
}
