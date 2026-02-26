package org.refit.refitbackend.domain.jobposting.entity;

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
import org.refit.refitbackend.domain.jobposting.entity.enums.CrawlStatus;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_posting_crawl_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPostCrawlLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "target_url", length = 1000)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlStatus status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Builder
    private JobPostCrawlLog(
            String source,
            String targetUrl,
            CrawlStatus status,
            Integer httpStatus,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        this.source = source;
        this.targetUrl = targetUrl;
        this.status = status;
        this.httpStatus = httpStatus;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.finishedAt = finishedAt;
    }

    public void markSuccess(Integer httpStatus) {
        this.status = CrawlStatus.SUCCESS;
        this.httpStatus = httpStatus;
        this.errorMessage = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFailed(Integer httpStatus, String errorMessage) {
        this.status = CrawlStatus.FAILED;
        this.httpStatus = httpStatus;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }
}
