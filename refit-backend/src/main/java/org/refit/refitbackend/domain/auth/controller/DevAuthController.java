package org.refit.refitbackend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.dto.AuthReq;
import org.refit.refitbackend.domain.auth.dto.AuthRes;
import org.refit.refitbackend.domain.auth.service.CustomOAuth2UserService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class DevAuthController {

    private final CustomOAuth2UserService oAuth2UserService;

    @Operation(summary = "개발용 토큰 발급", description = "사용자 ID로 AT/RT 발급 (개발 환경용)")
    @PostMapping("/dev/token")
    public ResponseEntity<ApiResponse<AuthRes.TokenDto>> issueDevToken(
            @Valid @RequestBody AuthReq.DevTokenRequest request
    ) {
        return ResponseUtil.ok("token_issued", oAuth2UserService.issueDevToken(request));
    }
}
