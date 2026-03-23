package org.refit.refitbackend.domain.notification.outbox.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationPushRequestedEvent;
import org.refit.refitbackend.domain.notification.kafka.event.NotificationRequestedEvent;
import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxMessage;
import org.refit.refitbackend.domain.notification.outbox.repository.NotificationOutboxRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.kafka.config.KafkaTopicProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    public static final String EVENT_NOTIFICATION_REQUESTED = "NOTIFICATION_REQUESTED";
    public static final String EVENT_NOTIFICATION_PUSH_REQUESTED = "NOTIFICATION_PUSH_REQUESTED";

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final KafkaTopicProperties topicProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public void appendNotificationRequested(NotificationRequestedEvent event) {
        notificationOutboxRepository.save(NotificationOutboxMessage.pending(
                EVENT_NOTIFICATION_REQUESTED,
                topicProperties.getNotificationRequested(),
                event.receiverId() != null ? String.valueOf(event.receiverId()) : null,
                toJson(event)
        ));
    }

    @Transactional
    public void appendNotificationPushRequested(NotificationPushRequestedEvent event) {
        notificationOutboxRepository.save(NotificationOutboxMessage.pending(
                EVENT_NOTIFICATION_PUSH_REQUESTED,
                topicProperties.getNotificationPushRequested(),
                event.receiverId() != null ? String.valueOf(event.receiverId()) : null,
                toJson(event)
        ));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }
    }
}
