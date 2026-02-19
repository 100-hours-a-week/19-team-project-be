package org.refit.refitbackend.domain.notification.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.notification.dto.NotificationReq;
import org.refit.refitbackend.domain.notification.dto.NotificationRes;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.notification.NotificationSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v2/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @NotificationSwaggerSpec.ListNotifications
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationRes.NotificationListResponse>> listNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ResponseUtil.ok("success",
                notificationService.listMyNotifications(principal.getUserId(), cursor, size));
    }

    @NotificationSwaggerSpec.ReadNotification
    @PatchMapping("/{notification_id}")
    public ResponseEntity<ApiResponse<NotificationRes.ReadNotificationResponse>> readNotification(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("notification_id") @Positive Long notificationId,
            @Valid @RequestBody NotificationReq.UpdateReadStatus request
    ) {
        return ResponseUtil.ok("success",
                notificationService.readNotification(principal.getUserId(), notificationId, request));
    }

    @NotificationSwaggerSpec.ReadAllNotifications
    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationRes.ReadAllNotificationsResponse>> readAllNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody NotificationReq.UpdateReadStatus request
    ) {
        return ResponseUtil.ok("success",
                notificationService.readAllNotifications(principal.getUserId(), request));
    }

    @NotificationSwaggerSpec.RegisterFcmToken
    @PostMapping("/fcm-tokens")
    public ResponseEntity<ApiResponse<NotificationRes.FcmTokenResponse>> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody NotificationReq.UpsertFcmToken request
    ) {
        return ResponseUtil.ok("success",
                notificationService.registerFcmToken(principal.getUserId(), request));
    }

    @NotificationSwaggerSpec.DeleteFcmToken
    @DeleteMapping("/fcm-tokens")
    public ResponseEntity<ApiResponse<NotificationRes.DeleteFcmTokenResponse>> deleteFcmToken(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody NotificationReq.DeleteFcmToken request
    ) {
        return ResponseUtil.ok("success",
                notificationService.deleteFcmToken(principal.getUserId(), request));
    }
}
