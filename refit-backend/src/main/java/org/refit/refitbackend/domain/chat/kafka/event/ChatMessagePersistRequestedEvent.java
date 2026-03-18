package org.refit.refitbackend.domain.chat.kafka.event;

public record ChatMessagePersistRequestedEvent(
        Long chatId,
        Long senderId,
        String messageType,
        String content,
        Long roomSequence,
        String clientMessageId
) {
}
