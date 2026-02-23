package org.refit.refitbackend.domain.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.notification.dto.NotificationReq;
import org.refit.refitbackend.domain.notification.dto.NotificationRes;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"dev", "local"})
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v2/dev/notifications")
public class DevNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/test-push")
    public ResponseEntity<ApiResponse<NotificationRes.TestPushResponse>> sendTestPush(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody NotificationReq.TestPush request
    ) {
        return ResponseUtil.ok("success",
                notificationService.sendTestPush(principal.getUserId(), request));
    }
}
