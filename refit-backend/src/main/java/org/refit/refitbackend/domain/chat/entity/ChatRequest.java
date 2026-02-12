package org.refit.refitbackend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "resume_id")
    private Long resumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private ChatRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRequestStatus status = ChatRequestStatus.PENDING;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "job_post_url", length = 500)
    private String jobPostUrl;

    @Builder
    private ChatRequest(User requester, User receiver, Long resumeId, ChatRequestType requestType, String jobPostUrl) {
        this.requester = requester;
        this.receiver = receiver;
        this.resumeId = resumeId;
        this.requestType = requestType;
        this.status = ChatRequestStatus.PENDING;
        this.jobPostUrl = jobPostUrl;
    }

    public void accept() {
        this.status = ChatRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ChatRequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}
