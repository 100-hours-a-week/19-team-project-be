package org.refit.refitbackend.global.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicProperties {

    private String resumeParseRequested = "resume.parse.requested";
    private String reportGenerateRequested = "report.generate.requested";
    private String resumeParseRequestedDlq = "resume.parse.requested.dlq";
    private String reportGenerateRequestedDlq = "report.generate.requested.dlq";

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
}
