package org.refit.refitbackend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.domain.user.service.UserService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
@Tag(name = "User", description = "유저 조회 API (프론트 디버깅용)")
public class UserController {

    private final UserService userService;

    /* =======================
     * 유저 단건 조회 (id)
     * ======================= */
    @Operation(
            summary = "유저 단건 조회",
            description = "유저 ID로 단건 조회"
    )
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserRes.Detail>> getUser(@PathVariable Long userId) {
        return ResponseUtil.ok("success", userService.getUser(userId));
    }

    /* =======================
     * 내 정보 조회
     * ======================= */
    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 사용자 정보 조회"
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserRes.Me>> getMe(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseUtil.ok("success", userService.getMe(principal.getUserId()));
    }
}
