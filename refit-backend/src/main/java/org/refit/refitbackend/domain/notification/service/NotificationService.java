package org.refit.refitbackend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.dto.NotificationReq;
import org.refit.refitbackend.domain.notification.dto.NotificationRes;
import org.refit.refitbackend.domain.notification.entity.FcmToken;
import org.refit.refitbackend.domain.notification.entity.Notification;
import org.refit.refitbackend.domain.notification.repository.FcmTokenRepository;
import org.refit.refitbackend.domain.notification.repository.NotificationRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.sse.SseService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;
    private final SseService sseService;

    public NotificationRes.NotificationListResponse listMyNotifications(Long userId, Long cursor, Integer size) {
        User user = getActiveUser(userId);

        int pageSize = normalizePageSize(size);
        List<Notification> notifications = notificationRepository.findByUserIdWithCursor(
                user.getId(),
                cursor,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasMore = notifications.size() > pageSize;
        if (hasMore) {
            notifications = notifications.subList(0, pageSize);
        }

        String nextCursor = notifications.isEmpty()
                ? null
                : String.valueOf(notifications.get(notifications.size() - 1).getId());

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        List<NotificationRes.NotificationItem> items = notifications.stream()
                .map(NotificationRes.NotificationItem::from)
                .toList();

        return new NotificationRes.NotificationListResponse(items, nextCursor, hasMore, unreadCount);
    }

    @Transactional
    public NotificationRes.ReadNotificationResponse readNotification(
            Long userId,
            Long notificationId,
            NotificationReq.UpdateReadStatus request
    ) {
        validateReadStatus(request);
        getActiveUser(userId);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.NOTIFICATION_NOT_FOUND));
        if (notification.isRead()) {
            throw new CustomException(ExceptionType.NOTIFICATION_ALREADY_READ);
        }
        notification.markRead();
        return NotificationRes.ReadNotificationResponse.from(notification);
    }

    @Transactional
    public NotificationRes.ReadAllNotificationsResponse readAllNotifications(
            Long userId,
            NotificationReq.UpdateReadStatus request
    ) {
        validateReadStatus(request);
        User user = getActiveUser(userId);
        int updatedCount = notificationRepository.markAllAsRead(user.getId(), LocalDateTime.now());
        return new NotificationRes.ReadAllNotificationsResponse(updatedCount);
    }

    @Transactional
    public NotificationRes.FcmTokenResponse registerFcmToken(Long userId, NotificationReq.UpsertFcmToken request) {
        User user = getActiveUser(userId);
        String token = request.token().trim();

        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .map(existing -> {
                    if (!existing.getUser().getId().equals(user.getId())) {
                        existing.changeOwner(user);
                    }
                    return existing;
                })
                .orElseGet(() -> fcmTokenRepository.save(FcmToken.builder()
                        .user(user)
                        .token(token)
                        .build()));

        return NotificationRes.FcmTokenResponse.from(fcmToken);
    }

    @Transactional
    public NotificationRes.DeleteFcmTokenResponse deleteFcmToken(Long userId, NotificationReq.DeleteFcmToken request) {
        getActiveUser(userId);
        long deletedCount = fcmTokenRepository.deleteByUserIdAndToken(userId, request.token().trim());
        return new NotificationRes.DeleteFcmTokenResponse(deletedCount > 0);
    }

    public NotificationRes.TestPushResponse sendTestPush(Long userId, NotificationReq.TestPush request) {
        getActiveUser(userId);
        String token = request.token().trim();
        String title = request.title().trim();
        String content = request.content().trim();
        FcmPushService.SendResult result = fcmPushService.sendToTokens(List.of(token), title, content);
        return new NotificationRes.TestPushResponse(
                result.successCount() > 0,
                token,
                title,
                result.requestedCount(),
                result.successCount(),
                result.failureCount(),
                result.attempted()
        );
    }

    @Transactional
    public void notifyChatRequestCreated(User requester, User receiver, Long chatRequestId) {
        String title = "새 채팅 요청이 도착했어요";
        String content = requester.getNickname() + "님이 채팅 요청을 보냈습니다.";
        String type = "CHAT_REQUEST_CREATED";

        sendNotification(receiver, type, title, content);
    }

    @Transactional
    public void notifyChatMessageReceived(User sender, User receiver, Long chatId, String messageContent) {
        String title = "새 메시지가 도착했어요";
        String preview = messageContent == null ? "" : messageContent.trim();
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "...";
        }
        String content = sender.getNickname() + ": " + preview;
        String type = "CHAT_MESSAGE_RECEIVED";
        sendNotification(receiver, type, title, content);
    }

    @Transactional
    public void notifyChatRequestAccepted(User requester, User receiver, Long chatRequestId, Long chatId) {
        String title = "채팅 요청이 수락되었어요";
        String content = receiver.getNickname() + "님이 채팅 요청을 수락했습니다.";
        String type = "CHAT_REQUEST_ACCEPTED";
        sendNotification(requester, type, title, content);
    }

    @Transactional
    public void notifyChatRequestRejected(User requester, User receiver, Long chatRequestId) {
        String title = "채팅 요청이 거절되었어요";
        String content = receiver.getNickname() + "님이 채팅 요청을 거절했습니다.";
        String type = "CHAT_REQUEST_REJECTED";
        sendNotification(requester, type, title, content);
    }

    @Transactional
    public void notifyResumeParseCompleted(Long userId, String taskId) {
        userRepository.findById(userId).ifPresent(user -> {
            String title = "이력서 분석이 완료됐어요";
            String content = "이력서 자동 분석이 완료되었습니다. (task_id: " + taskId + ")";
            sendNotification(user, "RESUME_PARSE_COMPLETED", title, content);
        });
    }

    @Transactional
    public void notifyResumeParseFailed(Long userId, String taskId, String reasonCode) {
        userRepository.findById(userId).ifPresent(user -> {
            String title = "이력서 분석에 실패했어요";
            String content = "다시 시도해주세요. (task_id: " + taskId + ", reason: " + reasonCode + ")";
            sendNotification(user, "RESUME_PARSE_FAILED", title, content);
        });
    }

    @Transactional
    public void notifyReportGenerateCompleted(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            String title = "AI 리포트 생성이 완료됐어요";
            String content = "리포트가 준비되었습니다.";
            sendNotification(user, "REPORT_GENERATE_COMPLETED", title, content);
        });
    }

    @Transactional
    public void notifyReportGenerateFailed(Long userId, String reasonCode) {
        userRepository.findById(userId).ifPresent(user -> {
            String title = "AI 리포트 생성에 실패했어요";
            String content = "요청을 처리하지 못했어요. " + toKoreanReason(reasonCode) + " 잠시 후 다시 시도해 주세요.";
            sendNotification(user, "REPORT_GENERATE_FAILED", title, content);
        });
    }

    private String toKoreanReason(String reasonCode) {
        if (reasonCode == null || reasonCode.isBlank()) {
            return "일시적인 오류가 발생했습니다.";
        }
        return switch (reasonCode) {
            case "JOB_POST_PARSE_FAILED" -> "채용 공고 내용을 읽지 못했습니다.";
            case "AI_SERVER_ERROR", "INTERNAL_SERVER_ERROR" -> "분석 서버 연결이 원활하지 않았습니다.";
            case "KAFKA_DISABLED", "KAFKA_PUBLISH_FAILED" -> "요청 접수 처리 중 문제가 발생했습니다.";
            default -> "일시적인 오류가 발생했습니다.";
        };
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new CustomException(ExceptionType.USER_DELETED);
        }
        return user;
    }

    private int normalizePageSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }

    private void validateReadStatus(NotificationReq.UpdateReadStatus request) {
        if (request == null || request.isRead() == null || !request.isRead()) {
            throw new CustomException(ExceptionType.NOTIFICATION_IS_READ_INVALID);
        }
    }

    private void sendNotification(User receiver, String type, String title, String content) {
        try {
            Notification savedNotification = notificationRepository.save(Notification.builder()
                    .user(receiver)
                    .type(type)
                    .title(title)
                    .content(content)
                    .isRead(false)
                    .build());

            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(receiver.getId());
            sseService.sendNotificationEvent(receiver.getId(), type, savedNotification.getId(), unreadCount);

            List<String> tokens = fcmTokenRepository.findAllByUserId(receiver.getId()).stream()
                    .map(FcmToken::getToken)
                    .distinct()
                    .toList();
            fcmPushService.sendToTokens(tokens, title, content);
        } catch (Exception e) {
            log.warn("Notification send failed. receiverId={}, type={}", receiver.getId(), type, e);
        }
    }
}
