package org.refit.refitbackend.domain.report.dto;

import org.refit.refitbackend.domain.report.entity.Report;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public class ReportRes {

    public record ReportId(
            Long reportId
    ) {}

    public record ReportListItem(
            Long reportId,
            String title,
            String status,
            Long chatRoomId,
            Long resumeId,
            String jobPostUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ReportListItem from(Report report) {
            return new ReportListItem(
                    report.getId(),
                    report.getTitle(),
                    report.getStatus().name(),
                    report.getChatRoomId(),
                    report.getResumeId(),
                    report.getJobPostUrl(),
                    report.getCreatedAt(),
                    report.getUpdatedAt()
            );
        }
    }

    public record ReportListResponse(
            List<ReportListItem> reports
    ) {}

    public record ReportDetail(
            Long reportId,
            Long userId,
            Long expertId,
            Long chatRoomId,
            Long chatFeedbackId,
            Long chatRequestId,
            Long resumeId,
            String title,
            String status,
            JsonNode resultJson,
            String jobPostUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
