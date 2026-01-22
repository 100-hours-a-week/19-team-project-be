package org.refit.refitbackend.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRes {

    @Schema(description = "채팅방 목록 응답")
    public record RoomListItem(
            @Schema(description = "채팅방 ID", example = "1")
            Long id,

            @Schema(description = "요청한 사람 정보")
            UserInfo requester,

            @Schema(description = "요청받은 사람 정보")
            UserInfo receiver,

            @Schema(description = "마지막 메시지 정보")
            LastMessageInfo lastMessage,

            @Schema(description = "읽지 않은 메시지 개수", example = "3")
            Long unreadCount,

            @Schema(description = "채팅방 상태", example = "ACTIVE")
            String status,

            @Schema(description = "생성 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,

            @Schema(description = "수정 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime updatedAt
    ) {
        public static RoomListItem from(ChatRoom room, Long unreadCount) {

            UserInfo requester = UserInfo.from(room.getRequester());
            UserInfo receiver = UserInfo.from(room.getReceiver());
            // 마지막 메시지 정보
            LastMessageInfo lastMessageInfo = room.getLastMessage() != null
                    ? LastMessageInfo.from(room.getLastMessage())
                    : null;

            return new RoomListItem(
                    room.getId(),
                    requester,
                    receiver,
                    lastMessageInfo,
                    unreadCount,
                    room.getStatus().name(),
                    room.getCreatedAt(),
                    room.getUpdatedAt()
            );
        }
    }

    @Schema(description = "채팅방 상세 정보")
    public record RoomDetail(
            @Schema(description = "채팅방 ID", example = "1")
            Long chatId,

            @Schema(description = "요청자 정보")
            UserInfo requester,

            @Schema(description = "수신자 정보")
            UserInfo receiver,

            @Schema(description = "이력서 ID", example = "1")
            Long resumeId,

            @Schema(description = "공고 URL", example = "https://example.com/job/123")
            String jobPostUrl,

            @Schema(description = "채팅방 상태", example = "ACTIVE")
            String status,

            @Schema(description = "생성 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,

            @Schema(description = "종료 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime closedAt
    ) {
        public static RoomDetail from(ChatRoom room) {
            return new RoomDetail(
                    room.getId(),
                    UserInfo.from(room.getRequester()),
                    UserInfo.from(room.getReceiver()),
                    room.getResumeId(),
                    room.getJobPostUrl(),
                    room.getStatus().name(),
                    room.getCreatedAt(),
                    room.getClosedAt()
            );
        }
    }

    @Schema(description = "메시지 정보")
    public record MessageInfo(
            @Schema(description = "메시지 ID", example = "1")
            Long messageId,

            @Schema(description = "채팅방 ID", example = "1")
            Long chatId,

            @Schema(description = "발신자 정보")
            UserInfo sender,

            @Schema(description = "메시지 타입", example = "TEXT")
            String messageType,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            String content,

            @Schema(description = "전송 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt
    ) {
        public static MessageInfo from(ChatMessage message) {
            return new MessageInfo(
                    message.getId(),
                    message.getChatRoom().getId(),
                    UserInfo.from(message.getSender()),
                    message.getMessageType().name(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }

    @Schema(description = "사용자 정보")
    public record UserInfo(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "홍길동")
            String nickname,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl,

            @Schema(description = "사용자 타입", example = "EXPERT")
            String userType
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getUserType().name()
            );
        }
    }

    @Schema(description = "마지막 메시지 정보")
    public record LastMessageInfo(
            @Schema(description = "메시지 ID", example = "1")
            Long id,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            String content,

            @Schema(description = "마지막 메시지 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime lastMessageAt
    ) {
        public static LastMessageInfo from(ChatMessage message) {
            return new LastMessageInfo(
                    message.getId(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }


    public record CreateChat(
            Long chatId
    ){
        public static CreateChat from(ChatRoom room) {
            return new CreateChat(
                    room.getId()
            );
        }
    }

    public record ChatCursorResponse(
            List<ChatRes.RoomListItem> chats,
            String nextCursor,
            boolean hasMore
    ) {}

    public record MessageCursorResponse(
            List<ChatRes.MessageInfo> messages,
            String nextCursor,
            boolean hasMore
    ) {}
}
