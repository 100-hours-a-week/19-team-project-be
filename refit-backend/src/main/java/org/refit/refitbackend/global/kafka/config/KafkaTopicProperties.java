package org.refit.refitbackend.global.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicProperties {

    private String resumeParseRequested = "resume.parse.requested";
    private String reportGenerateRequested = "report.generate.requested";
    private String mentorEmbeddingRefreshRequested = "mentor.embedding.refresh.requested";
    private String resumeParseRequestedDlq = "resume.parse.requested.dlq";
    private String reportGenerateRequestedDlq = "report.generate.requested.dlq";
    private String mentorEmbeddingRefreshRequestedDlq = "mentor.embedding.refresh.requested.dlq";

    public String getResumeParseRequested() {
        return resumeParseRequested;
    }

    public void setResumeParseRequested(String resumeParseRequested) {
        this.resumeParseRequested = resumeParseRequested;
    }

    public String getReportGenerateRequested() {
        return reportGenerateRequested;
    }

    public void setReportGenerateRequested(String reportGenerateRequested) {
        this.reportGenerateRequested = reportGenerateRequested;
    }

    public String getResumeParseRequestedDlq() {
        return resumeParseRequestedDlq;
    }

    public void setResumeParseRequestedDlq(String resumeParseRequestedDlq) {
        this.resumeParseRequestedDlq = resumeParseRequestedDlq;
    }

    public String getReportGenerateRequestedDlq() {
        return reportGenerateRequestedDlq;
    }

    public void setReportGenerateRequestedDlq(String reportGenerateRequestedDlq) {
        this.reportGenerateRequestedDlq = reportGenerateRequestedDlq;
    }

    public String getMentorEmbeddingRefreshRequested() {
        return mentorEmbeddingRefreshRequested;
    }

    public void setMentorEmbeddingRefreshRequested(String mentorEmbeddingRefreshRequested) {
        this.mentorEmbeddingRefreshRequested = mentorEmbeddingRefreshRequested;
    }

    public String getMentorEmbeddingRefreshRequestedDlq() {
        return mentorEmbeddingRefreshRequestedDlq;
    }

    public void setMentorEmbeddingRefreshRequestedDlq(String mentorEmbeddingRefreshRequestedDlq) {
        this.mentorEmbeddingRefreshRequestedDlq = mentorEmbeddingRefreshRequestedDlq;
    }
}
