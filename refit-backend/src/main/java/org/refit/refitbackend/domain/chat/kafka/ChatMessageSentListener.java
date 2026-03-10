package org.refit.refitbackend.domain.chat.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.MessageType;
import org.refit.refitbackend.domain.chat.kafka.event.ChatMessageSentEvent;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.notification.kafka.NotificationEventPublisher;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationRequestedEvent;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.global.sse.SseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class ChatMessageSentListener {

    private final Optional<NotificationEventPublisher> notificationEventPublisher;
    private final NotificationService notificationService;
    private final SseService sseService;
    private final ChatRoomRepository chatRoomRepository;

    @Value("${app.notification.async.enabled:false}")
    private boolean notificationAsyncEnabled;

    @KafkaListener(
            topics = "${app.kafka.topics.chat-message-sent:chat.message.sent}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    public void onMessageSent(ChatMessageSentEvent event) {
        try {
            if (event == null || event.receiverId() == null || event.chatId() == null || event.senderId() == null) {
                return;
            }

            if (MessageType.SYSTEM.name().equals(event.messageType())) {
                return;
            }

            long unreadCount = resolveUnreadCount(event.chatId(), event.receiverId(), event.roomSequence());
            sseService.sendChatEvent(
                    event.receiverId(),
                    event.chatId(),
                    event.messageId() != null ? event.messageId() : 0L,
                    unreadCount
            );

            handleNotification(event);
        } catch (Exception e) {
            log.warn("Kafka chat message side-effect failed. chatId={}, senderId={}, receiverId={}",
                    event.chatId(), event.senderId(), event.receiverId(), e);
        }
    }

    private void handleNotification(ChatMessageSentEvent event) {
        String title = "새 메시지가 도착했어요";
        String content = buildMessagePreview(event.content());
        String type = "CHAT_MESSAGE_RECEIVED";

        if (notificationAsyncEnabled && notificationEventPublisher.isPresent()) {
            notificationEventPublisher.get().publishNotificationRequested(
                    new NotificationRequestedEvent(
                            type,
                            event.senderId(),
                            event.receiverId(),
                            event.chatId(),
                            event.content()
                    )
            );
            return;
        }

        // fallback도 DB 조회 없이 push-only로 처리해 메시지 경로 DB 부하를 줄인다.
        notificationService.sendPushOnly(event.receiverId(), title, content, type);
    }

    private String buildMessagePreview(String messageContent) {
        String preview = messageContent == null ? "" : messageContent.trim();
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "...";
        }
        return preview.isBlank() ? "새 채팅 메시지가 도착했습니다." : preview;
    }

    private long resolveUnreadCount(Long chatId, Long receiverId, Long roomSequence) {
        if (roomSequence == null) {
            return 0L;
        }

        ChatRoom room = chatRoomRepository.findById(chatId).orElse(null);
        if (room == null) {
            return 0L;
        }

        long lastReadSeq = 0L;
        if (room.getRequester().getId().equals(receiverId)) {
            lastReadSeq = room.getRequesterLastReadSeq() != null ? room.getRequesterLastReadSeq() : 0L;
        } else if (room.getReceiver().getId().equals(receiverId)) {
            lastReadSeq = room.getReceiverLastReadSeq() != null ? room.getReceiverLastReadSeq() : 0L;
        }

        return Math.max(0L, roomSequence - lastReadSeq);
    }
}
