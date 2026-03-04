package org.refit.refitbackend.domain.task.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.task.kafka.event.MentorEmbeddingRefreshRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.global.kafka.config.KafkaTopicProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TaskEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void publishResumeParseRequested(ResumeParseRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getResumeParseRequested(), event.taskId(), event);
        log.info("Kafka published resume parse request. taskId={}, userId={}", event.taskId(), event.userId());
    }

    public void publishReportGenerateRequested(ReportGenerateRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getReportGenerateRequested(), event.taskId(), event);
        log.info("Kafka published report generate request. taskId={}, userId={}", event.taskId(), event.userId());
    }

    public void publishMentorEmbeddingRefreshRequested(MentorEmbeddingRefreshRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getMentorEmbeddingRefreshRequested(), event.taskId(), event);
        log.info("Kafka published mentor embedding refresh request. taskId={}, userId={}", event.taskId(), event.userId());
    }
}
