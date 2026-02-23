package org.refit.refitbackend.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import org.refit.refitbackend.domain.chat.entity.ChatFeedback;
import org.refit.refitbackend.domain.chat.entity.ChatFeedbackAnswer;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRequest;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.user.entity.User;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRes {

    @Schema(description = "채팅방 목록 응답")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RoomListItem(
            @Schema(description = "채팅방 ID", example = "1")
            Long chatId,

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
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RoomDetail(
            @Schema(description = "채팅방 ID", example = "1")
            Long chatId,

            @Schema(description = "요청자 정보")
            UserInfo requester,

            @Schema(description = "수신자 정보")
            UserInfo receiver,

            @Schema(description = "이력서 ID", example = "1")
            Long resumeId,

            @Schema(description = "이력서 정보")
            ResumeInfo resume,

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
        public static RoomDetail from(ChatRoom room, ResumeInfo resume) {
            return new RoomDetail(
                    room.getId(),
                    UserInfo.from(room.getRequester()),
                    UserInfo.from(room.getReceiver()),
                    room.getResumeId(),
                    resume,
                    room.getJobPostUrl(),
                    room.getStatus().name(),
                    room.getCreatedAt(),
                    room.getClosedAt()
            );
        }
    }

    @Schema(description = "이력서 정보")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ResumeInfo(
            @Schema(description = "이력서 ID", example = "1")
            Long resumeId,

            @Schema(description = "이력서 제목")
            String title,

            @Schema(description = "신입 여부")
            Boolean isFresher,

            @Schema(description = "학력")
            String educationLevel,

            @Schema(description = "이력서 내용")
            JsonNode contentJson,

            @Schema(description = "생성 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,

            @Schema(description = "수정 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime updatedAt
    ) {}

    @Schema(description = "메시지 정보")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MessageInfo(
            @Schema(description = "메시지 ID", example = "1")
            Long messageId,

            @Schema(description = "채팅방 ID", example = "1")
            Long chatId,

            @Schema(description = "채팅방 내 메시지 시퀀스", example = "100")
            Long roomSequence,

            @Schema(description = "발신자 정보")
            UserInfo sender,

            @Schema(description = "메시지 타입", example = "TEXT")
            String messageType,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            String content,

            @Schema(description = "클라이언트 메시지 ID", example = "cmsg_1700000000000")
            String clientMessageId,

            @Schema(description = "전송 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt
    ) {
        public static MessageInfo from(ChatMessage message) {
            return new MessageInfo(
                    message.getId(),
                    message.getChatRoom().getId(),
                    message.getRoomSequence(),
                    UserInfo.from(message.getSender()),
                    message.getMessageType().name(),
                    message.getContent(),
                    message.getClientMessageId(),
                    message.getCreatedAt()
            );
        }
    }

    @Schema(description = "사용자 정보")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LastMessageInfo(
            @Schema(description = "메시지 ID", example = "1")
            Long messageId,

            @Schema(description = "채팅방 내 메시지 시퀀스(읽음 처리 기준)", example = "100")
            Long roomSequence,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            String content,

            @Schema(description = "마지막 메시지 시각")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime lastMessageAt
    ) {
        public static LastMessageInfo from(ChatMessage message) {
            return new LastMessageInfo(
                    message.getId(),
                    message.getRoomSequence(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }


    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateChat(
            Long chatId
    ) {
        public static CreateChat from(ChatRoom room) {
            return new CreateChat(
                    room.getId()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatCursorResponse(
            List<ChatRes.RoomListItem> chats,
            String nextCursor,
            boolean hasMore
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MessageCursorResponse(
            List<ChatRes.MessageInfo> messages,
            String nextCursor,
            boolean hasMore
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatRequestId(
            Long chatRequestId
    ) {
        public static ChatRequestId from(ChatRequest request) {
            return new ChatRequestId(request.getId());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatRequestItem(
            Long chatRequestId,
            UserInfo requester,
            UserInfo receiver,
            Long resumeId,
            String requestType,
            String status,
            String jobPostUrl,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime respondedAt
    ) {
        public static ChatRequestItem from(ChatRequest request) {
            return new ChatRequestItem(
                    request.getId(),
                    UserInfo.from(request.getRequester()),
                    UserInfo.from(request.getReceiver()),
                    request.getResumeId(),
                    request.getRequestType().name(),
                    request.getStatus().name(),
                    request.getJobPostUrl(),
                    request.getCreatedAt(),
                    request.getRespondedAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatRequestCursorResponse(
            List<ChatRequestItem> requests,
            String nextCursor,
            boolean hasMore
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RespondRequestResult(
            Long chatRequestId,
            String status,
            Long chatId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatFeedbackId(
            Long chatFeedbackId,
            Long chatId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatFeedbackAnswerItem(
            Long questionId,
            String questionKey,
            String questionText,
            String answerType,
            Integer displayOrder,
            String answerValue
    ) {
        public static ChatFeedbackAnswerItem from(ChatFeedbackAnswer answer) {
            return new ChatFeedbackAnswerItem(
                    answer.getQuestion().getId(),
                    answer.getQuestion().getQuestionKey(),
                    answer.getQuestion().getQuestionText(),
                    answer.getQuestion().getAnswerType(),
                    answer.getQuestion().getDisplayOrder(),
                    answer.getAnswerValue()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatFeedbackDetail(
            Long chatFeedbackId,
            Long chatId,
            UserInfo expert,
            UserInfo user,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,
            List<ChatFeedbackAnswerItem> answers
    ) {
        public static ChatFeedbackDetail from(ChatFeedback feedback, List<ChatFeedbackAnswer> answers) {
            return new ChatFeedbackDetail(
                    feedback.getId(),
                    feedback.getChatRoom().getId(),
                    UserInfo.from(feedback.getExpert()),
                    UserInfo.from(feedback.getUser()),
                    feedback.getCreatedAt(),
                    answers.stream().map(ChatFeedbackAnswerItem::from).toList()
            );
        }
    }
}
