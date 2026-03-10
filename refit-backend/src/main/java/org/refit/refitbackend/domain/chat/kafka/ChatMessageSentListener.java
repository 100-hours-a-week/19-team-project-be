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
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

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
        if (notificationAsyncEnabled && notificationEventPublisher.isPresent()) {
            notificationEventPublisher.get().publishNotificationRequested(
                    new NotificationRequestedEvent(
                            "CHAT_MESSAGE_RECEIVED",
                            event.senderId(),
                            event.receiverId(),
                            event.chatId(),
                            event.content()
                    )
            );
            return;
        }

        User sender = userRepository.findById(event.senderId()).orElse(null);
        User receiver = userRepository.findById(event.receiverId()).orElse(null);
        if (sender == null || receiver == null) {
            return;
        }
        notificationService.notifyChatMessageReceived(sender, receiver, event.chatId(), event.content());
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
