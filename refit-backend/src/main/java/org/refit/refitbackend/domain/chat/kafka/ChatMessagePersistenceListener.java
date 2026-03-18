package org.refit.refitbackend.domain.chat.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.MessageType;
import org.refit.refitbackend.domain.chat.kafka.event.ChatMessagePersistRequestedEvent;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.chat.persistence.async.enabled"}, havingValue = "true")
public class ChatMessagePersistenceListener {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EntityManager entityManager;

    @Value("${app.chat.persistence.async.batch-size:100}")
    private int batchSize;

    @Value("${app.chat.persistence.async.flush-interval-ms:200}")
    private long flushIntervalMs;

    private final List<ChatMessagePersistRequestedEvent> buffer = new ArrayList<>();
    private Instant lastFlushAt = Instant.now();

    @KafkaListener(
            topics = "${app.kafka.topics.chat-message-persist-requested:chat.message.persist.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    @Transactional
    public void onPersistRequested(ChatMessagePersistRequestedEvent event) {
        List<ChatMessagePersistRequestedEvent> batchToFlush = null;
        synchronized (buffer) {
            buffer.add(event);
            if (buffer.size() >= batchSize || shouldFlushByTime()) {
                batchToFlush = drainBufferLocked();
            }
        }
        if (batchToFlush != null) {
            persistBatch(batchToFlush);
        }
    }

    @Scheduled(fixedDelayString = "${app.chat.persistence.async.flush-interval-ms:200}")
    @Transactional
    public void flushIfNeeded() {
        List<ChatMessagePersistRequestedEvent> batchToFlush = null;
        synchronized (buffer) {
            if (!buffer.isEmpty() && shouldFlushByTime()) {
                batchToFlush = drainBufferLocked();
            }
        }
        if (batchToFlush != null) {
            persistBatch(batchToFlush);
        }
    }

    private boolean shouldFlushByTime() {
        return Duration.between(lastFlushAt, Instant.now()).toMillis() >= flushIntervalMs;
    }

    private List<ChatMessagePersistRequestedEvent> drainBufferLocked() {
        if (buffer.isEmpty()) {
            return null;
        }

        List<ChatMessagePersistRequestedEvent> batch = new ArrayList<>(buffer);
        buffer.clear();
        lastFlushAt = Instant.now();
        return batch;
    }

    void persistBatch(List<ChatMessagePersistRequestedEvent> batch) {
        List<ChatMessagePersistRequestedEvent> candidates = batch.stream()
                .filter(e -> e.chatId() != null && e.senderId() != null && e.roomSequence() != null)
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        Set<String> uniqueRoomSeqKeys = new HashSet<>();
        List<ChatMessagePersistRequestedEvent> deduped = new ArrayList<>();
        for (ChatMessagePersistRequestedEvent event : candidates) {
            String key = event.chatId() + "::" + event.roomSequence();
            if (uniqueRoomSeqKeys.add(key)) {
                deduped.add(event);
            }
        }

        Map<Long, List<Long>> roomSeqMap = deduped.stream()
                .collect(Collectors.groupingBy(
                        ChatMessagePersistRequestedEvent::chatId,
                        Collectors.mapping(ChatMessagePersistRequestedEvent::roomSequence, Collectors.toList())
                ));

        Set<String> existingRoomSeqKeys = new HashSet<>();
        for (Map.Entry<Long, List<Long>> entry : roomSeqMap.entrySet()) {
            List<ChatMessage> existingMessages = chatMessageRepository.findAllByChatRoom_IdAndRoomSequenceIn(
                    entry.getKey(),
                    entry.getValue()
            );
            for (ChatMessage message : existingMessages) {
                existingRoomSeqKeys.add(message.getChatRoom().getId() + "::" + message.getRoomSequence());
            }
        }

        List<ChatMessage> toSave = new ArrayList<>();
        Map<Long, ChatRoom> roomMap = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();

        for (ChatMessagePersistRequestedEvent event : deduped) {
            String roomSeqKey = event.chatId() + "::" + event.roomSequence();
            if (existingRoomSeqKeys.contains(roomSeqKey)) {
                continue;
            }

            ChatRoom room = roomMap.computeIfAbsent(
                    event.chatId(),
                    id -> chatRoomRepository.getReferenceById(id)
            );
            User sender = userMap.computeIfAbsent(
                    event.senderId(),
                    id -> entityManager.getReference(User.class, id)
            );

            MessageType messageType;
            try {
                messageType = MessageType.valueOf(event.messageType());
            } catch (Exception e) {
                messageType = MessageType.TEXT;
            }

            toSave.add(ChatMessage.builder()
                    .chatRoom(room)
                    .sender(sender)
                    .messageType(messageType)
                    .content(event.content())
                    .roomSequence(event.roomSequence())
                    .clientMessageId(event.clientMessageId())
                    .build());
        }

        if (!toSave.isEmpty()) {
            List<ChatMessage> savedMessages = chatMessageRepository.saveAll(toSave);
            updateChatRoomState(savedMessages);
            log.debug("Kafka consumed chat persist batch. requested={}, accepted={}, inserted={}",
                    batch.size(), deduped.size(), savedMessages.size());
        }
    }

    private void updateChatRoomState(Collection<ChatMessage> savedMessages) {
        Map<Long, List<ChatMessage>> groupedByRoom = savedMessages.stream()
                .collect(Collectors.groupingBy(m -> m.getChatRoom().getId()));

        for (List<ChatMessage> roomMessages : groupedByRoom.values()) {
            roomMessages.sort((a, b) -> Long.compare(a.getRoomSequence(), b.getRoomSequence()));
            ChatMessage latest = roomMessages.get(roomMessages.size() - 1);
            ChatRoom room = latest.getChatRoom();

            Long currentSeq = room.getLastMessageSeq() != null ? room.getLastMessageSeq() : 0L;
            if (latest.getRoomSequence() >= currentSeq) {
                room.updateLastMessage(latest);
            }

            for (ChatMessage saved : roomMessages) {
                room.updateLastReadMessage(saved.getSender().getId(), saved);
            }
        }
    }
}
