package org.refit.refitbackend.domain.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.refit.refitbackend.domain.report.entity.enums.ReportStatus;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.util.Map;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "chat_feedback_id", nullable = false)
    private Long chatFeedbackId;

    @Column(name = "chat_request_id", nullable = false)
    private Long chatRequestId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "title", nullable = false, length = 20)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "result_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> resultJson;

    @Column(name = "job_post_url", nullable = false, length = 500)
    private String jobPostUrl;

    @Builder
    private Report(
            Long userId,
            Long expertId,
            Long chatRoomId,
            Long chatFeedbackId,
            Long chatRequestId,
            Long resumeId,
            String title,
            ReportStatus status,
            Map<String, Object> resultJson,
            String jobPostUrl
    ) {
        this.userId = userId;
        this.expertId = expertId;
        this.chatRoomId = chatRoomId;
        this.chatFeedbackId = chatFeedbackId;
        this.chatRequestId = chatRequestId;
        this.resumeId = resumeId;
        this.title = title;
        this.status = status;
        this.resultJson = resultJson;
        this.jobPostUrl = jobPostUrl;
    }

    public void markProcessing() {
        this.status = ReportStatus.PROCESSING;
    }

    public void markCompleted(Map<String, Object> resultJson) {
        this.status = ReportStatus.COMPLETED;
        this.resultJson = resultJson;
    }

    public void markFailed() {
        this.status = ReportStatus.FAILED;
    }
}
