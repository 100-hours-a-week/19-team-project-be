package org.refit.refitbackend.domain.auth.dto;

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
            @NotBlank(message = "인증 코드가 필요합니다.")
            @Schema(description = "카카오 인가 코드")
            String code
    ) {}


    /* =======================
     * 회원가입 (OAuth 정보 + 추가 정보 한번에)
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record SignUp(
            @NotNull(message = "유효하지 않은 OAuth 제공자입니다.")
            @Schema(description = "OAuth Provider", example = "KAKAO")
            OAuthProvider oauthProvider,

            @NotBlank(message = "OAuth ID가 필요합니다.")
            @Schema(description = "OAuth 고유 ID")
            String oauthId,

            @Email(message = "회원가입 이메일 형식이 올바르지 않습니다.")
            @Schema(description = "이메일")
            String email,

            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(min = 2, max = 10, message = "닉네임 길이가 올바르지 않습니다.")
            @Schema(description = "닉네임")
            String nickname,

            @NotNull(message = "회원가입 사용자 유형이 올바르지 않습니다.")
            @Schema(description = "사용자 유형", example = "JOB_SEEKER", name = "user_type")
            UserType userType,

            @NotNull(message = "경력 레벨을 찾을 수 없습니다.")
            @Schema(description = "경력 레벨 ID", name = "career_level_id", example = "1")
            Long careerLevelId,

            @Schema(description = "직무 ID 목록", example = "[1, 2]")
            List<Long> jobIds,

            @Schema(description = "스킬 목록")
            List<SkillRequest> skills,

            @Schema(description = "자기소개", example = "백엔드 개발자입니다.")
            String introduction,

            @Schema(description = "회사명 (현직자 선택 시 선택)", example = "네이버")
            String companyName,

            @Schema(description = "회사 이메일 (현직자 선택 시 선택)", example = "user@navercorp.com")
            String companyEmail
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
            @NotBlank(message = "리프레시 토큰이 필요합니다.")
            @Schema(description = "Refresh Token")
            String refreshToken
    ) {}

    /* =======================
     * 로그아웃 요청
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record LogoutRequest(
            @Schema(description = "Refresh Token")
            String refreshToken
    ) {}

    /* =======================
     * 탈퇴 계정 복구 요청
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record Restore(
            @NotNull(message = "유효하지 않은 OAuth 제공자입니다.")
            @Schema(description = "OAuth Provider", example = "KAKAO")
            OAuthProvider oauthProvider,

            @NotBlank(message = "OAuth ID가 필요합니다.")
            @Schema(description = "OAuth 고유 ID")
            String oauthId,

            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Schema(description = "이메일")
            String email,

            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(min = 2, max = 10, message = "닉네임 길이가 올바르지 않습니다.")
            @Schema(description = "닉네임")
            String nickname,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl
    ) {}

    /* =======================
     * 개발용 토큰 발급 요청
     * ======================= */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static record DevTokenRequest(
            @NotNull(message = "사용자 ID가 필요합니다.")
            @Schema(description = "User ID")
            Long userId
    ) {}
}
