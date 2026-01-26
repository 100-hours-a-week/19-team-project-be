package org.refit.refitbackend.domain.auth.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationReq;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationRes;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.auth.service.EmailVerificationService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.auth.AuthSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email Verification", description = "회사 이메일 인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @AuthSwaggerSpec.SendEmailVerificationCode
    @SecurityRequirement(name = "accessToken")
    @PostMapping
    public ResponseEntity<ApiResponse<EmailVerificationRes.Send>> sendCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EmailVerificationReq.Send request
    ) {
        EmailVerificationRes.Send response = emailVerificationService.sendVerificationCode(principal.getUserId(), request.email());
        return ResponseUtil.ok("verification_code_sent", response);
    }

    @AuthSwaggerSpec.SendEmailVerificationCodePublic
    @PostMapping("/public")
    public ResponseEntity<ApiResponse<EmailVerificationRes.Send>> sendCodePublic(
            @Valid @RequestBody EmailVerificationReq.Send request
    ) {
        EmailVerificationRes.Send response = emailVerificationService.sendVerificationCodePublic(request.email());
        return ResponseUtil.ok("verification_code_sent", response);
    }

    @AuthSwaggerSpec.VerifyEmailCode
    @SecurityRequirement(name = "accessToken")
    @PatchMapping
    public ResponseEntity<ApiResponse<EmailVerificationRes.Verify>> verifyCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EmailVerificationReq.Verify request
    ) {
        EmailVerificationRes.Verify response = emailVerificationService.verifyCode(principal.getUserId(), request.email(), request.code());
        return ResponseUtil.ok("verification_success", response);
    }

    @AuthSwaggerSpec.VerifyEmailCodePublic
    @PatchMapping("/public")
    public ResponseEntity<ApiResponse<EmailVerificationRes.Verify>> verifyCodePublic(
            @Valid @RequestBody EmailVerificationReq.Verify request
    ) {
        EmailVerificationRes.Verify response = emailVerificationService.verifyCodePublic(request.email(), request.code());
        return ResponseUtil.ok("verification_success", response);
    }
}
