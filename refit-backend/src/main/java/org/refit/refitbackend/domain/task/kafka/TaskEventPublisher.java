package org.refit.refitbackend.domain.task.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.task.kafka.event.MentorEmbeddingRefreshRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.task.outbox.service.TaskOutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TaskEventPublisher {

    private final TaskOutboxService taskOutboxService;

    public void publishResumeParseRequested(ResumeParseRequestedEvent event) {
        taskOutboxService.appendResumeParseRequested(event);
        log.info("Task outbox appended resume parse request. taskId={}, userId={}", event.taskId(), event.userId());
    }

    public void publishReportGenerateRequested(ReportGenerateRequestedEvent event) {
        taskOutboxService.appendReportGenerateRequested(event);
        log.info("Task outbox appended report generate request. taskId={}, userId={}", event.taskId(), event.userId());
    }

    public void publishMentorEmbeddingRefreshRequested(MentorEmbeddingRefreshRequestedEvent event) {
        taskOutboxService.appendMentorEmbeddingRefreshRequested(event);
        log.info("Task outbox appended mentor embedding refresh request. taskId={}, userId={}", event.taskId(), event.userId());
    }

    public void publishMentorEmbeddingRefreshRequested(MentorEmbeddingRefreshRequestedEvent event) {
        kafkaTemplate.send(topicProperties.getMentorEmbeddingRefreshRequested(), event.taskId(), event);
        log.info("Kafka published mentor embedding refresh request. taskId={}, userId={}", event.taskId(), event.userId());
    }
}
