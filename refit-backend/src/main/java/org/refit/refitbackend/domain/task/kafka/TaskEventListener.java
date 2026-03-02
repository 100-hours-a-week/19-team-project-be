package org.refit.refitbackend.domain.task.kafka;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.report.service.ReportService;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.resume.service.ResumeTaskService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TaskEventListener {

    private final ResumeTaskService resumeTaskService;
    private final ReportService reportService;

    @KafkaListener(
            topics = "${app.kafka.topics.resume-parse-requested:resume.parse.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    public void onResumeParseRequested(ResumeParseRequestedEvent event) {
        resumeTaskService.processAsyncParseTask(event);
        log.info("Kafka consumed resume parse request. taskId={}, userId={}, fileUrl={}",
                event.taskId(), event.userId(), event.fileUrl());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.report-generate-requested:report.generate.requested}",
            groupId = "${spring.kafka.consumer.group-id:refit-backend}"
    )
    public void onReportGenerateRequested(ReportGenerateRequestedEvent event) {
        reportService.processAsyncGenerateReportTask(event);
        log.info("Kafka consumed report generate request. taskId={}, userId={}, reportId={}, chatRoomId={}",
                event.taskId(), event.userId(), event.reportId(), event.chatRoomId());
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
}
