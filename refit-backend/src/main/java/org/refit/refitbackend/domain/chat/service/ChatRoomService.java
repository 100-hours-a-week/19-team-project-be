package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRequest;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.entity.MessageType;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRequestRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.chat.repository.projection.ChatMessageCursorProjection;
import org.refit.refitbackend.domain.chat.repository.projection.ChatRoomListProjection;
import org.refit.refitbackend.domain.chat.repository.projection.ChatRoomReadStateProjection;
import org.refit.refitbackend.domain.report.entity.enums.ReportStatus;
import org.refit.refitbackend.domain.report.repository.ReportRepository;
import org.refit.refitbackend.domain.resume.entity.Resume;
import org.refit.refitbackend.domain.resume.repository.ResumeRepository;
import org.refit.refitbackend.domain.storage.dto.StorageReq;
import org.refit.refitbackend.domain.storage.service.StorageService;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ReportRepository reportRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 생성
     */
    @Transactional
    public ChatRes.CreateChat createRoom(Long requesterId, ChatReq.CreateRoom request) {
        validateResumeOwnership(requesterId, request.resumeId());

        // 요청자 조회
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        // 수신자 조회
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        // 본인과 채팅 불가
        if (requesterId.equals(request.receiverId())) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        // 이미 활성 채팅방 존재 체크
        chatRoomRepository.findActiveRoomBetweenUsers(requesterId, request.receiverId())
                .ifPresent(room -> {
                    throw new CustomException(ExceptionType.CHAT_ROOM_ALREADY_EXISTS);
                });

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .requester(requester)
                .receiver(receiver)
                .resumeId(request.resumeId())
                .jobPostUrl(request.jobPostUrl())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        return ChatRes.CreateChat.from(savedRoom);
    }

    private void validateResumeOwnership(Long requesterId, Long resumeId) {
        if (resumeId == null) {
            return;
        }
        resumeRepository.findByIdAndUserId(resumeId, requesterId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));
    }

    /**
     * 내 채팅방 목록 조회
     */
    public CursorPage<ChatRes.RoomListItem> getMyChats(
            Long userId,
            ChatRoomStatus status,
            Long cursorId,
            int size
    ) {
        List<ChatRoomListProjection> rows = chatRoomRepository.findMyRoomSummariesByCursor(
                userId,
                status,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = rows.size() > size;
        if (hasMore) {
            rows = rows.subList(0, size);
        }

        List<ChatRes.RoomListItem> items = rows.stream()
                .map(this::toRoomListItem)
                .toList();

        String nextCursor = rows.isEmpty() ? null : String.valueOf(rows.get(rows.size() - 1).getChatId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    private ChatRes.RoomListItem toRoomListItem(ChatRoomListProjection row) {
        ChatRes.UserInfo requester = new ChatRes.UserInfo(
                row.getRequesterId(),
                row.getRequesterNickname(),
                row.getRequesterProfileImageUrl(),
                row.getRequesterUserType().name()
        );
        ChatRes.UserInfo receiver = new ChatRes.UserInfo(
                row.getReceiverId(),
                row.getReceiverNickname(),
                row.getReceiverProfileImageUrl(),
                row.getReceiverUserType().name()
        );
        ChatRes.LastMessageInfo lastMessage = row.getLastMessageId() == null
                ? null
                : new ChatRes.LastMessageInfo(
                        row.getLastMessageId(),
                        row.getLastMessageRoomSequence(),
                        row.getLastMessageContent(),
                        row.getLastMessageAt()
                );

        String requestType = row.getRequestType() != null ? row.getRequestType().name() : null;

        return new ChatRes.RoomListItem(
                row.getChatId(),
                requester,
                receiver,
                lastMessage,
                row.getUnreadCount(),
                row.getStatus().name(),
                requestType,
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    /**
     * 채팅방 상세 조회
     */
    public ChatRes.RoomDetail getRoomDetail(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        ChatRes.ResumeInfo resumeInfo = null;
        Long resumeId = room.getResumeId();
        if (resumeId != null) {
            Resume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume != null) {
                JsonNode contentJson = null;
                String rawContent = resume.getContentJson();
                if (rawContent != null && !rawContent.isBlank()) {
                    try {
                        contentJson = objectMapper.readTree(rawContent);
                    } catch (Exception ignored) {
                        contentJson = null;
                    }
                }
                resumeInfo = new ChatRes.ResumeInfo(
                        resume.getId(),
                        resume.getTitle(),
                        resume.getIsFresher(),
                        resume.getEducationLevel(),
                        contentJson,
                        resume.getCreatedAt(),
                        resume.getUpdatedAt()
                );
            }
        }

        String requestType = null;
        if (room.getChatRequestId() != null) {
            requestType = chatRequestRepository.findById(room.getChatRequestId())
                    .map(request -> request.getRequestType().name())
                    .orElse(null);
        }

        boolean hasReport = reportRepository.existsByChatRoomIdAndStatusIn(
                room.getId(),
                List.of(ReportStatus.PROCESSING, ReportStatus.COMPLETED)
        );

        return ChatRes.RoomDetail.from(room, resumeInfo, requestType, hasReport);
    }

    /**
     * 채팅방 이력서 원본 다운로드용 presigned URL 발급
     */
    public PresignedUrlResponse getResumeDownloadUrl(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        Long resumeId = room.getResumeId();
        if (resumeId == null) {
            throw new CustomException(ExceptionType.RESUME_NOT_FOUND);
        }

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));

        String fileUrl = resume.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new CustomException(ExceptionType.RESUME_NOT_FOUND);
        }

        StorageReq.PresignedDownloadRequest request = new StorageReq.PresignedDownloadRequest(fileUrl);
        return storageService.issuePresignedDownloadUrl(userId, request);
    }

    /**
     * 채팅방 종료
     */
    @Transactional
    public void closeRoom(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            throw new CustomException(ExceptionType.CHAT_ALREADY_CLOSED);
        }

        room.close();

        ChatMessage systemMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(room.getReceiver())
                .messageType(MessageType.SYSTEM)
                .content("채팅이 종료되었습니다.")
                .roomSequence(room.nextMessageSequence())
                .build());
        room.updateLastMessage(systemMessage);
    }

    /**
     * 메시지 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, Long roomId, ChatReq.ReadMessage request) {
        ChatRoomReadStateProjection room = chatRoomRepository.findReadStateByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        Long lastReadSeq = request.lastReadSeq();
        Long lastMessageSeq = room.getLastMessageSeq();
        if (lastMessageSeq == null) {
            return;
        }
        if (lastReadSeq > lastMessageSeq) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        if (room.getRequesterId().equals(userId)) {
            chatRoomRepository.updateRequesterLastReadSeqIfGreater(roomId, userId, lastReadSeq);
            return;
        }
        if (room.getReceiverId().equals(userId)) {
            chatRoomRepository.updateReceiverLastReadSeqIfGreater(roomId, userId, lastReadSeq);
            return;
        }
        throw new CustomException(ExceptionType.AUTH_FORBIDDEN);
    }

    /**
     * 채팅방 메시지 목록 조회 (커서)
     */
    public CursorPage<ChatRes.MessageInfo> getMessages(
            Long userId,
            Long roomId,
            Long cursorId,
        int size
    ) {
        List<ChatMessageCursorProjection> messages = chatMessageRepository.findMessageSummariesByChatIdAndUserIdByCursor(
                roomId,
                userId,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        if (messages.isEmpty() && !chatRoomRepository.existsByIdAndUserId(roomId, userId)) {
            throw new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND);
        }

        boolean hasMore = messages.size() > size;
        if (hasMore) {
            messages = messages.subList(0, size);
        }

        List<ChatRes.MessageInfo> items = messages.stream()
                .map(this::toMessageInfo)
                .toList();

        String nextCursor = messages.isEmpty() ? null : String.valueOf(messages.get(messages.size() - 1).getMessageId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    private ChatRes.MessageInfo toMessageInfo(ChatMessageCursorProjection row) {
        ChatRes.UserInfo sender = new ChatRes.UserInfo(
                row.getSenderId(),
                row.getSenderNickname(),
                row.getSenderProfileImageUrl(),
                row.getSenderUserType().name()
        );
        return new ChatRes.MessageInfo(
                row.getMessageId(),
                row.getChatId(),
                row.getRoomSequence(),
                sender,
                row.getMessageType().name(),
                row.getContent(),
                row.getClientMessageId(),
                row.getCreatedAt()
        );
    }
}
