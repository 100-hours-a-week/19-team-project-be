package org.refit.refitbackend.domain.task.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.task.kafka.event.MentorEmbeddingRefreshRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxMessage;
import org.refit.refitbackend.domain.task.outbox.repository.TaskOutboxRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.kafka.config.KafkaTopicProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskOutboxService {

    public static final String EVENT_RESUME_PARSE_REQUESTED = "RESUME_PARSE_REQUESTED";
    public static final String EVENT_REPORT_GENERATE_REQUESTED = "REPORT_GENERATE_REQUESTED";
    public static final String EVENT_MENTOR_EMBEDDING_REFRESH_REQUESTED = "MENTOR_EMBEDDING_REFRESH_REQUESTED";

    private final TaskOutboxRepository taskOutboxRepository;
    private final KafkaTopicProperties topicProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public void appendResumeParseRequested(ResumeParseRequestedEvent event) {
        taskOutboxRepository.save(TaskOutboxMessage.pending(
                EVENT_RESUME_PARSE_REQUESTED,
                topicProperties.getResumeParseRequested(),
                event.taskId(),
                toJson(event)
        ));
    }

    @Transactional
    public void appendReportGenerateRequested(ReportGenerateRequestedEvent event) {
        taskOutboxRepository.save(TaskOutboxMessage.pending(
                EVENT_REPORT_GENERATE_REQUESTED,
                topicProperties.getReportGenerateRequested(),
                event.taskId(),
                toJson(event)
        ));
    }

    @Transactional
    public void appendMentorEmbeddingRefreshRequested(MentorEmbeddingRefreshRequestedEvent event) {
        taskOutboxRepository.save(TaskOutboxMessage.pending(
                EVENT_MENTOR_EMBEDDING_REFRESH_REQUESTED,
                topicProperties.getMentorEmbeddingRefreshRequested(),
                event.taskId(),
                toJson(event)
        ));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }
    }
}
