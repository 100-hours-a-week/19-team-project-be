package org.refit.refitbackend.global.swagger.spec.notification;

import org.refit.refitbackend.domain.notification.dto.NotificationReq;
import org.refit.refitbackend.domain.notification.dto.NotificationRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiConflictError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class NotificationSwaggerSpec {

    private NotificationSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "알림 목록 조회",
            operationDescription = "로그인 사용자의 알림 목록을 커서 기반으로 조회합니다.",
            implementation = NotificationRes.NotificationListResponse.class
    )
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface ListNotifications {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "단일 알림 읽음 처리",
            operationDescription = "특정 알림을 읽음 처리합니다.",
            implementation = NotificationRes.ReadNotificationResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.NOTIFICATION_IS_READ_INVALID
    })
    @SwaggerApiNotFoundError(description = "notification_not_found", types = {
            ExceptionType.NOTIFICATION_NOT_FOUND
    })
    @SwaggerApiConflictError(types = {
            ExceptionType.NOTIFICATION_ALREADY_READ
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = NotificationReq.UpdateReadStatus.class,
            examples = { "{ \"is_read\": true }" },
            exampleNames = { "read_notification" }
    )
    public @interface ReadNotification {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "전체 알림 읽음 처리",
            operationDescription = "읽지 않은 알림을 모두 읽음 처리합니다.",
            implementation = NotificationRes.ReadAllNotificationsResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.NOTIFICATION_IS_READ_INVALID
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = NotificationReq.UpdateReadStatus.class,
            examples = { "{ \"is_read\": true }" },
            exampleNames = { "read_all_notifications" }
    )
    public @interface ReadAllNotifications {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "FCM 토큰 등록",
            operationDescription = "푸시 알림 수신용 FCM 토큰을 등록합니다.",
            implementation = NotificationRes.FcmTokenResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = NotificationReq.UpsertFcmToken.class,
            examples = { "{ \"token\": \"fcm_device_token\" }" },
            exampleNames = { "register_fcm_token" }
    )
    public @interface RegisterFcmToken {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "FCM 토큰 삭제",
            operationDescription = "푸시 알림 수신용 FCM 토큰을 삭제합니다.",
            implementation = NotificationRes.DeleteFcmTokenResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = NotificationReq.DeleteFcmToken.class,
            examples = { "{ \"token\": \"fcm_device_token\" }" },
            exampleNames = { "delete_fcm_token" }
    )
    public @interface DeleteFcmToken {}
}
