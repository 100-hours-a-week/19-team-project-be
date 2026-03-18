package org.refit.refitbackend.domain.chat.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.kafka.event.ChatMessagePersistRequestedEvent;
import org.refit.refitbackend.domain.chat.kafka.event.ChatMessageSentEvent;
import org.refit.refitbackend.global.kafka.config.KafkaTopicProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class ChatMessageEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void publishMessageSent(ChatMessageSentEvent event) {
        kafkaTemplate.send(topicProperties.getChatMessageSent(), String.valueOf(event.chatId()), event);
        log.debug("Kafka published chat message. chatId={}, messageId={}, senderId={}",
                event.chatId(), event.messageId(), event.senderId());
    }

    public void publishPersistRequested(ChatMessagePersistRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getChatMessagePersistRequested(), String.valueOf(event.chatId()), event);
        log.debug("Kafka published chat persist request. chatId={}, senderId={}, roomSequence={}",
                event.chatId(), event.senderId(), event.roomSequence());
    }
}
