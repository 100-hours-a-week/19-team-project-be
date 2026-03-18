package org.refit.refitbackend.domain.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationRequestedEvent;
import org.refit.refitbackend.global.kafka.config.KafkaTopicProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.notification.async.enabled"}, havingValue = "true")
public class NotificationEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void publishNotificationRequested(NotificationRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getNotificationRequested(), String.valueOf(event.receiverId()), event);
        log.debug("Kafka published notification request. type={}, senderId={}, receiverId={}, chatId={}",
                event.type(), event.senderId(), event.receiverId(), event.chatId());
    }
}
