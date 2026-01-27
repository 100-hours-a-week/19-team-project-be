package org.refit.refitbackend.domain.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.domain.user.service.UserService;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.user.UserSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "User", description = "유저 조회 API")
public class UserController {

    private final UserService userService;

    /* =======================
     * 유저 단건 조회 (id)
     * ======================= */
    @UserSwaggerSpec.GetUser
    @GetMapping("/{user_id}")
    public ResponseEntity<ApiResponse<UserRes.Detail>> getUser(
            @Parameter(description = "유저 ID", example = "1", required = true)
            @PathVariable("user_id") Long userId
    ) {
        return ResponseUtil.ok("success", userService.getUser(userId));
    }

    /* =======================
     * 내 정보 조회
     * ======================= */
    @UserSwaggerSpec.GetMe
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserRes.Me>> getMe(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseUtil.ok("success", userService.getMe(principal.getUserId()));
    }

    /**
     * 닉네임 중복 검사
     */
    @UserSwaggerSpec.CheckNickname
    @GetMapping(params = "nickname")
    public ResponseEntity<ApiResponse<UserRes.NicknameCheck>> checkNickname(
            @Parameter(description = "닉네임", example = "테스터", required = true)
            @RequestParam
            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(min = 2, max = 10, message = "닉네임 길이가 올바르지 않습니다.")
            String nickname
    ) {
        return ResponseUtil.ok("success", userService.checkNickname(nickname));
    }

    /**
     * 전체 유저 검색ㅈ
     */
    @UserSwaggerSpec.SearchUsers
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<UserRes.UserCursorResponse>> searchUsers(
            @Parameter(description = "검색 키워드")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "직무 ID")
            @RequestParam(required = false) Long jobId,
            @Parameter(description = "스킬 ID")
            @RequestParam(required = false) Long skillId,
            @Parameter(description = "커서(마지막 유저 ID)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size
    ) {
        CursorPage<UserRes.UserSearch> page = userService.searchUsers(keyword, jobId, skillId, cursor, size);
        UserRes.UserCursorResponse res = new UserRes.UserCursorResponse(page.items(), page.nextCursor(), page.hasMore());
        return ResponseUtil.ok("success", res);
    }
}
