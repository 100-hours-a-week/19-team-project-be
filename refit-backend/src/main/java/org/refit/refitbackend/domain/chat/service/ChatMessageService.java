package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.kafka.ChatMessageEventPublisher;
import org.refit.refitbackend.domain.chat.kafka.event.ChatMessagePersistRequestedEvent;
import org.refit.refitbackend.domain.chat.kafka.event.ChatMessageSentEvent;
import org.refit.refitbackend.domain.chat.realtime.ChatRealtimePublisher;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.entity.MessageType;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.sse.SseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final NotificationService notificationService;
    private final SseService sseService;
    private final Optional<ChatMessageEventPublisher> chatMessageEventPublisher;
    @Value("${app.chat.persistence.async.enabled:false}")
    private boolean asyncPersistenceEnabled;

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatRes.MessageInfo sendMessage(Long senderId, ChatReq.SendMessage request) {
        // 채팅방 조회 및 권한 체크
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(request.chatId(), senderId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new CustomException(ExceptionType.CHAT_ALREADY_CLOSED);
        }

        User senderRef = resolveSender(chatRoom, senderId);
        Long receiverId = chatRoom.getRequester().getId().equals(senderId)
                ? chatRoom.getReceiver().getId()
                : chatRoom.getRequester().getId();

        long roomSequence = chatRoom.nextMessageSequence();

        // 메시지 타입 결정
        MessageType messageType = request.messageType() != null
                ? MessageType.valueOf(request.messageType())
                : MessageType.TEXT;

        String clientMessageId = request.clientMessageId();
        if (clientMessageId == null || clientMessageId.isBlank()) {
            clientMessageId = "srv-" + request.chatId() + "-" + roomSequence;
        }

        ChatRes.MessageInfo payload;
        Long messageIdForEvent = null;
        Long messageIdForSse = 0L;
        String content = request.content();

        if (asyncPersistenceEnabled && chatMessageEventPublisher.isPresent()) {
            // Phase2: DB 저장은 Kafka 소비자에서 처리, 요청 트랜잭션에서는 실시간 전달만 수행
            chatRoom.updateLastReadSeq(senderId, roomSequence);
            payload = new ChatRes.MessageInfo(
                    null,
                    request.chatId(),
                    roomSequence,
                    // 비동기 경로는 sender id만 있으면 클라이언트 측 매핑 가능 (불필요한 DB user 조회 방지)
                    new ChatRes.UserInfo(senderId, null, null, null),
                    messageType.name(),
                    content,
                    clientMessageId,
                    LocalDateTime.now()
            );

            ChatMessagePersistRequestedEvent persistEvent = new ChatMessagePersistRequestedEvent(
                    request.chatId(),
                    senderId,
                    messageType.name(),
                    content,
                    roomSequence,
                    clientMessageId
            );
            chatMessageEventPublisher.get().publishPersistRequested(persistEvent);
        } else {
            // fallback: Kafka 미사용/비활성 환경은 기존 동기 저장 유지
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(senderRef)
                    .messageType(messageType)
                    .content(content)
                    .roomSequence(roomSequence)
                    .clientMessageId(clientMessageId)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(message);

            chatRoom.updateLastMessage(savedMessage);
            chatRoom.updateLastReadMessage(senderId, savedMessage);

            payload = ChatRes.MessageInfo.from(savedMessage);
            messageIdForEvent = savedMessage.getId();
            messageIdForSse = savedMessage.getId();
        }

        // 실시간 fan-out은 Redis Pub/Sub(활성화 시) 또는 로컬 브로커(비활성화 시)로 전송
        chatRealtimePublisher.publish(request.chatId(), payload);
        log.debug("메시지 전송 성공 - roomId: {}, senderId: {}", request.chatId(), senderId);

        // 상대방 알림/SSE는 Kafka 이벤트 소비 경로에서 비동기 처리
        if (messageType != MessageType.SYSTEM) {
            if (chatMessageEventPublisher.isPresent()) {
                ChatMessageEventPublisher publisher = chatMessageEventPublisher.get();
                publisher.publishMessageSent(
                        new ChatMessageSentEvent(
                                request.chatId(),
                                messageIdForEvent,
                                roomSequence,
                                senderId,
                                receiverId,
                                messageType.name(),
                                content,
                                clientMessageId
                        )
                );
            } else {
                // Kafka 비활성화 환경에서는 기존 동기 흐름 유지
                User sender = userRepository.findById(senderId).orElse(null);
                User receiver = userRepository.findById(receiverId).orElse(null);
                if (sender != null && receiver != null) {
                    long unreadCount = calculateUnreadCount(chatRoom, receiver.getId());
                    sseService.sendChatEvent(receiver.getId(), request.chatId(), messageIdForSse, unreadCount);
                    notificationService.notifyChatMessageReceived(sender, receiver, request.chatId(), content);
                }
            }
        }

        return payload;
    }

    private User resolveSender(ChatRoom chatRoom, Long senderId) {
        if (chatRoom.getRequester().getId().equals(senderId)) {
            return chatRoom.getRequester();
        }
        if (chatRoom.getReceiver().getId().equals(senderId)) {
            return chatRoom.getReceiver();
        }
        throw new CustomException(ExceptionType.AUTH_FORBIDDEN);
    }

    private long calculateUnreadCount(ChatRoom chatRoom, Long userId) {
        long lastMessageSeq = chatRoom.getLastMessageSeq() != null ? chatRoom.getLastMessageSeq() : 0L;
        long lastReadSeq = resolveLastReadSeq(chatRoom, userId);
        return Math.max(0L, lastMessageSeq - lastReadSeq);
    }

    private long resolveLastReadSeq(ChatRoom chatRoom, Long userId) {
        if (chatRoom.getRequester().getId().equals(userId)) {
            return chatRoom.getRequesterLastReadSeq() != null ? chatRoom.getRequesterLastReadSeq() : 0L;
        }
        if (chatRoom.getReceiver().getId().equals(userId)) {
            return chatRoom.getReceiverLastReadSeq() != null ? chatRoom.getReceiverLastReadSeq() : 0L;
        }
        return 0L;
    }
}
