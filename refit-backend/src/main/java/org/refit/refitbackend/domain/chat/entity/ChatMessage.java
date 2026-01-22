package org.refit.refitbackend.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_room_created", columnList = "chat_room_id, created_at"),
        @Index(name = "idx_chat_room_seq", columnList = "chat_room_id, room_sequence")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "room_sequence", nullable = false)
    private Long roomSequence;

    @Builder
    private ChatMessage(ChatRoom chatRoom, User sender, MessageType messageType, String content, Long roomSequence) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
        this.roomSequence = roomSequence;
    }
}
