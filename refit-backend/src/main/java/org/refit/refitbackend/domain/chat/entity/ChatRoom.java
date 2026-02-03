package org.refit.refitbackend.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;  // 요청자 (구직자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;   // 수신자 (현직자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private ChatMessage lastMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_seq")
    private Long lastMessageSeq;

    @Column(name = "requester_last_read_seq")
    private Long requesterLastReadSeq;

    @Column(name = "receiver_last_read_seq")
    private Long receiverLastReadSeq;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // V1에서는 chat_request 없이 바로 채팅방 생성하므로 nullable
    @Column(name = "chat_request_id")
    private Long chatRequestId;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "job_post_url", length = 500)
    private String jobPostUrl;

    @Builder
    private ChatRoom(User requester, User receiver, Long resumeId, String jobPostUrl) {
        this.requester = requester;
        this.receiver = receiver;
        this.resumeId = resumeId;
        this.jobPostUrl = jobPostUrl;
        this.status = ChatRoomStatus.ACTIVE;
    }

    // 마지막 메시지 업데이트
    public void updateLastMessage(ChatMessage message) {
        this.lastMessage = message;
        this.lastMessageAt = message.getCreatedAt();
        this.lastMessageSeq = message.getRoomSequence();
    }

    // 읽음 처리 (seq 기반)
    public void updateLastReadMessage(Long userId, ChatMessage message) {
        if (message == null) {
            return;
        }
        updateLastReadSeq(userId, message.getRoomSequence());
    }

    public void updateLastReadSeq(Long userId, Long lastReadSeq) {
        if (lastReadSeq == null) {
            return;
        }
        if (this.requester.getId().equals(userId)) {
            long current = this.requesterLastReadSeq != null ? this.requesterLastReadSeq : 0L;
            this.requesterLastReadSeq = Math.max(current, lastReadSeq);
        } else if (this.receiver.getId().equals(userId)) {
            long current = this.receiverLastReadSeq != null ? this.receiverLastReadSeq : 0L;
            this.receiverLastReadSeq = Math.max(current, lastReadSeq);
        }
    }

    public long nextMessageSequence() {
        long next = (this.lastMessageSeq == null ? 1L : this.lastMessageSeq + 1L);
        this.lastMessageSeq = next;
        return next;
    }

    // 채팅방 종료
    public void close() {
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}
