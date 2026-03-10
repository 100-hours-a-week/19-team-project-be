package org.refit.refitbackend.domain.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationPushRequestedEvent;
import org.refit.refitbackend.global.kafka.config.KafkaTopicProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.notification.async.enabled"}, havingValue = "true")
public class NotificationPushEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void publish(NotificationPushRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getNotificationPushRequested(), String.valueOf(event.receiverId()), event);
        log.debug("Kafka published notification push request. receiverId={}, notificationId={}, type={}",
                event.receiverId(), event.notificationId(), event.type());
    }
}
