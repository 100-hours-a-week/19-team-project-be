package org.refit.refitbackend.domain.chat.kafka.event;

public record ChatMessageSentEvent(
        Long chatId,
        Long messageId,
        Long roomSequence,
        Long senderId,
        Long receiverId,
        String messageType,
        String content,
        String clientMessageId
) {
}

