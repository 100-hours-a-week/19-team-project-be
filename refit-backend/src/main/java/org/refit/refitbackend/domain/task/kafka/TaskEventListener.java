package org.refit.refitbackend.domain.task.kafka;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.service.ExpertService;
import org.refit.refitbackend.domain.report.service.ReportService;
import org.refit.refitbackend.domain.task.kafka.event.MentorEmbeddingRefreshRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.resume.service.ResumeTaskService;
import org.refit.refitbackend.global.idempotency.service.ProcessedEventService;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TaskEventListener {

    private static final String RESUME_PARSE_CONSUMER = "task.resume-parse-requested";
    private static final String REPORT_GENERATE_CONSUMER = "task.report-generate-requested";
    private static final String MENTOR_EMBEDDING_REFRESH_CONSUMER = "task.mentor-embedding-refresh-requested";

    private final ResumeTaskService resumeTaskService;
    private final ReportService reportService;
    private final ExpertService expertService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "${app.kafka.topics.resume-parse-requested:resume.parse.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    @Transactional
    public void onResumeParseRequested(ResumeParseRequestedEvent event) {
        if (event == null || event.taskId() == null) {
            return;
        }
        if (!processedEventService.tryMarkProcessed(RESUME_PARSE_CONSUMER, event.taskId())) {
            log.info("Skip duplicate resume parse request. taskId={}", event.taskId());
            return;
        }
        resumeTaskService.processAsyncParseTask(event);
        log.info("Kafka consumed resume parse request. taskId={}, userId={}, fileUrl={}",
                event.taskId(), event.userId(), event.fileUrl());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.report-generate-requested:report.generate.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    @Transactional
    public void onReportGenerateRequested(ReportGenerateRequestedEvent event) {
        if (event == null || event.taskId() == null) {
            return;
        }
        if (!processedEventService.tryMarkProcessed(REPORT_GENERATE_CONSUMER, event.taskId())) {
            log.info("Skip duplicate report generate request. taskId={}", event.taskId());
            return;
        }
        reportService.processAsyncGenerateReportTask(event);
        log.info("Kafka consumed report generate request. taskId={}, userId={}, reportId={}, chatRoomId={}",
                event.taskId(), event.userId(), event.reportId(), event.chatRoomId());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.mentor-embedding-refresh-requested:mentor.embedding.refresh.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    @Transactional
    public void onMentorEmbeddingRefreshRequested(MentorEmbeddingRefreshRequestedEvent event) {
        if (event == null || event.taskId() == null) {
            return;
        }
        if (!processedEventService.tryMarkProcessed(MENTOR_EMBEDDING_REFRESH_CONSUMER, event.taskId())) {
            log.info("Skip duplicate mentor embedding refresh request. taskId={}", event.taskId());
            return;
        }
        try {
            expertService.refreshMentorEmbedding(event.userId());
            log.info("Kafka consumed mentor embedding refresh request. taskId={}, userId={}",
                    event.taskId(), event.userId());
        } catch (CustomException e) {
            if (e.getExceptionType() == ExceptionType.EXPERT_NOT_FOUND) {
                throw new IllegalArgumentException("Expert not found for embedding refresh. userId=" + event.userId());
            }
            throw e;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.resume-parse-requested-dlq:resume.parse.requested.dlq}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}-dlq"
    )
    public void onResumeParseRequestedDlq(ResumeParseRequestedEvent event, @Headers Map<String, Object> headers) {
        String reasonCode = resolveResumeDlqReasonCode(headers);
        log.error("Kafka DLQ consumed resume parse request. taskId={}, userId={}, reasonCode={}",
                event.taskId(), event.userId(), reasonCode);
        resumeTaskService.markAsyncParseTaskFailedFromDlq(event.taskId(), reasonCode);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.report-generate-requested-dlq:report.generate.requested.dlq}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}-dlq"
    )
    public void onReportGenerateRequestedDlq(ReportGenerateRequestedEvent event, @Headers Map<String, Object> headers) {
        String reasonCode = resolveReportDlqReasonCode(headers);
        log.error("Kafka DLQ consumed report generate request. reportId={}, userId={}, reasonCode={}",
                event.reportId(), event.userId(), reasonCode);
        reportService.markAsyncGenerateReportFailedFromDlq(event.taskId(), event.userId(), event.reportId(), reasonCode);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.mentor-embedding-refresh-requested-dlq:mentor.embedding.refresh.requested.dlq}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}-dlq"
    )
    public void onMentorEmbeddingRefreshRequestedDlq(MentorEmbeddingRefreshRequestedEvent event, @Headers Map<String, Object> headers) {
        String reasonCode = resolveMentorEmbeddingDlqReasonCode(headers);
        log.error("Kafka DLQ consumed mentor embedding refresh request. taskId={}, userId={}, reasonCode={}",
                event.taskId(), event.userId(), reasonCode);
    }

    private String resolveResumeDlqReasonCode(Map<String, Object> headers) {
        String exceptionMessage = headerAsString(headers, "kafka_dlt-exception-message");
        String stacktrace = headerAsString(headers, "kafka_dlt-exception-stacktrace");

        if (stacktrace.contains("UnknownHostException")) {
            return "AI_UNKNOWN_HOST";
        }
        if (stacktrace.contains("SocketTimeoutException")) {
            return "AI_TIMEOUT";
        }
        if (stacktrace.contains("ResourceAccessException")) {
            return "AI_CONNECTION_ERROR";
        }
        if (stacktrace.contains("RESUME_PARSE_FAILED") || stacktrace.contains("이력서 파싱에 실패했습니다")) {
            return "RESUME_PARSE_FAILED";
        }
        if (exceptionMessage.contains("서버 오류가 발생했습니다")) {
            return "INTERNAL_SERVER_ERROR";
        }
        return "KAFKA_RETRY_EXHAUSTED";
    }

    private String headerAsString(Map<String, Object> headers, String key) {
        Object value = headers.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private String resolveReportDlqReasonCode(Map<String, Object> headers) {
        String exceptionMessage = headerAsString(headers, "kafka_dlt-exception-message");
        String stacktrace = headerAsString(headers, "kafka_dlt-exception-stacktrace");

        if (stacktrace.contains("UnknownHostException")) {
            return "AI_UNKNOWN_HOST";
        }
        if (stacktrace.contains("SocketTimeoutException")) {
            return "AI_TIMEOUT";
        }
        if (stacktrace.contains("RESOURCE_EXHAUSTED") || stacktrace.contains("429")) {
            return "AI_RATE_LIMIT";
        }
        if (stacktrace.contains("GOOGLE_API_KEY environment variable is not set")) {
            return "AI_MISCONFIG";
        }
        if (exceptionMessage.contains("서버 오류가 발생했습니다")) {
            return "INTERNAL_SERVER_ERROR";
        }
        return "KAFKA_RETRY_EXHAUSTED";
    }

    private String resolveMentorEmbeddingDlqReasonCode(Map<String, Object> headers) {
        String exceptionMessage = headerAsString(headers, "kafka_dlt-exception-message");
        String stacktrace = headerAsString(headers, "kafka_dlt-exception-stacktrace");

        if (stacktrace.contains("UnknownHostException")) {
            return "AI_UNKNOWN_HOST";
        }
        if (stacktrace.contains("SocketTimeoutException")) {
            return "AI_TIMEOUT";
        }
        if (stacktrace.contains("ResourceAccessException")) {
            return "AI_CONNECTION_ERROR";
        }
        if (stacktrace.contains("EXPERT_NOT_FOUND")) {
            return "EXPERT_NOT_FOUND";
        }
        if (exceptionMessage.contains("서버 오류가 발생했습니다")) {
            return "INTERNAL_SERVER_ERROR";
        }
        return "KAFKA_RETRY_EXHAUSTED";
    }
}
