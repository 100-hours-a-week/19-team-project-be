package org.refit.refitbackend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationReq;
import org.refit.refitbackend.domain.auth.dto.EmailVerificationRes;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.auth.service.EmailVerificationService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.response.ErrorResponse;
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

    @Operation(summary = "이메일 인증 코드 발송", description = "회사 이메일로 6자리 인증번호를 발송합니다.")
    @SecurityRequirement(name = "accessToken")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EmailVerificationReq.Send.class),
                    examples = @ExampleObject(
                            name = "send_request",
                            value = "{ \"email\": \"user@navercorp.com\" }"
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "verification_code_sent",
                    content = @Content(
                            schema = @Schema(implementation = EmailVerificationRes.Send.class),
                            examples = @ExampleObject(
                                    name = "verification_code_sent",
                                    value = "{ \"code\": \"OK\", \"message\": \"verification_code_sent\", \"data\": { \"email\": \"user@navercorp.com\", \"expires_at\": \"2025-01-07T10:10:00\", \"sent_count\": 1, \"remaining_attempts\": 2 } }"
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "rate_limit",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "rate_limit",
                                    value = "{ \"code\": \"EMAIL_VERIFICATION_RATE_LIMIT\", \"message\": \"too many verification attempts\", \"data\": { \"retry_after_seconds\": 600 } }"
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<EmailVerificationRes.Send>> sendCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EmailVerificationReq.Send request
    ) {
        EmailVerificationRes.Send response = emailVerificationService.sendVerificationCode(principal.getUserId(), request.email());
        return ResponseUtil.ok("verification_code_sent", response);
    }

    @Operation(summary = "이메일 인증 코드 발송 (회원가입용)", description = "회원가입 단계에서 이메일로 6자리 인증번호를 발송합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EmailVerificationReq.Send.class),
                    examples = @ExampleObject(
                            name = "send_request",
                            value = "{ \"email\": \"user@navercorp.com\" }"
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "verification_code_sent",
                    content = @Content(schema = @Schema(implementation = EmailVerificationRes.Send.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "rate_limit",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/public")
    public ResponseEntity<ApiResponse<EmailVerificationRes.Send>> sendCodePublic(
            @Valid @RequestBody EmailVerificationReq.Send request
    ) {
        EmailVerificationRes.Send response = emailVerificationService.sendVerificationCodePublic(request.email());
        return ResponseUtil.ok("verification_code_sent", response);
    }

    @Operation(summary = "이메일 인증 코드 확인", description = "발송된 인증번호를 검증하고 인증을 완료합니다.")
    @SecurityRequirement(name = "accessToken")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EmailVerificationReq.Verify.class),
                    examples = @ExampleObject(
                            name = "verify_request",
                            value = "{ \"email\": \"user@navercorp.com\", \"code\": \"123456\" }"
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "verification_success",
                    content = @Content(
                            schema = @Schema(implementation = EmailVerificationRes.Verify.class),
                            examples = @ExampleObject(
                                    name = "verification_success",
                                    value = "{ \"code\": \"OK\", \"message\": \"verification_success\", \"data\": { \"email\": \"user@navercorp.com\", \"verified_at\": \"2025-01-12T10:00:00\" } }"
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410",
                    description = "verification_code_expired",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping
    public ResponseEntity<ApiResponse<EmailVerificationRes.Verify>> verifyCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EmailVerificationReq.Verify request
    ) {
        EmailVerificationRes.Verify response = emailVerificationService.verifyCode(principal.getUserId(), request.email(), request.code());
        return ResponseUtil.ok("verification_success", response);
    }

    @Operation(summary = "이메일 인증 코드 확인 (회원가입용)", description = "회원가입 단계에서 발송된 인증번호를 검증합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EmailVerificationReq.Verify.class),
                    examples = @ExampleObject(
                            name = "verify_request",
                            value = "{ \"email\": \"user@navercorp.com\", \"code\": \"123456\" }"
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "verification_success",
                    content = @Content(schema = @Schema(implementation = EmailVerificationRes.Verify.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410",
                    description = "verification_code_expired",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/public")
    public ResponseEntity<ApiResponse<EmailVerificationRes.Verify>> verifyCodePublic(
            @Valid @RequestBody EmailVerificationReq.Verify request
    ) {
        EmailVerificationRes.Verify response = emailVerificationService.verifyCodePublic(request.email(), request.code());
        return ResponseUtil.ok("verification_success", response);
    }
}
