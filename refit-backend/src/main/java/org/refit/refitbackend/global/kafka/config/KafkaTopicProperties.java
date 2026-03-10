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
    private String chatMessageSent = "chat.message.sent";
    private String notificationRequested = "notification.requested";
    private String notificationRequestedDlq = "notification.requested.dlq";
    private String notificationPushRequested = "notification.push.requested";
    private String notificationPushRequestedDlq = "notification.push.requested.dlq";
    private String chatMessagePersistRequested = "chat.message.persist.requested";
    private String chatMessagePersistRequestedDlq = "chat.message.persist.requested.dlq";

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

    public String getChatMessageSent() {
        return chatMessageSent;
    }

    public void setChatMessageSent(String chatMessageSent) {
        this.chatMessageSent = chatMessageSent;
    }

    public String getChatMessagePersistRequested() {
        return chatMessagePersistRequested;
    }

    public void setChatMessagePersistRequested(String chatMessagePersistRequested) {
        this.chatMessagePersistRequested = chatMessagePersistRequested;
    }

    public String getChatMessagePersistRequestedDlq() {
        return chatMessagePersistRequestedDlq;
    }

    public void setChatMessagePersistRequestedDlq(String chatMessagePersistRequestedDlq) {
        this.chatMessagePersistRequestedDlq = chatMessagePersistRequestedDlq;
    }

    public String getNotificationRequested() {
        return notificationRequested;
    }

    public void setNotificationRequested(String notificationRequested) {
        this.notificationRequested = notificationRequested;
    }

    public String getNotificationRequestedDlq() {
        return notificationRequestedDlq;
    }

    public void setNotificationRequestedDlq(String notificationRequestedDlq) {
        this.notificationRequestedDlq = notificationRequestedDlq;
    }

    public String getNotificationPushRequested() {
        return notificationPushRequested;
    }

    public void setNotificationPushRequested(String notificationPushRequested) {
        this.notificationPushRequested = notificationPushRequested;
    }

    public String getNotificationPushRequestedDlq() {
        return notificationPushRequestedDlq;
    }

    public void setNotificationPushRequestedDlq(String notificationPushRequestedDlq) {
        this.notificationPushRequestedDlq = notificationPushRequestedDlq;
    }
}
