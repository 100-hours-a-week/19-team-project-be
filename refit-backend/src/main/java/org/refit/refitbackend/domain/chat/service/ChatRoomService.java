package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 생성
     */
    @Transactional
    public ChatRes.CreateChat createRoom(Long requesterId, ChatReq.CreateRoom request) {
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

    /**
     * 내 채팅방 목록 조회
     */
    public CursorPage<ChatRes.RoomListItem> getMyChats(
            Long userId,
            ChatRoomStatus status,
            Long cursorId,
            int size
    ) {
        List<ChatRoom> rooms = chatRoomRepository.findMyRoomsByCursor(
                userId,
                status,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = rooms.size() > size;
        if (hasMore) {
            rooms = rooms.subList(0, size);
        }

        List<ChatRes.RoomListItem> items = rooms.stream()
                .map(r -> {
                    if (r.getLastMessageSeq() == null) {
                        return ChatRes.RoomListItem.from(r, 0L);
                    }
                    Long lastReadSeq = r.getRequester().getId().equals(userId)
                            ? r.getRequesterLastReadSeq()
                            : r.getReceiverLastReadSeq();
                    long lastRead = lastReadSeq != null ? lastReadSeq : 0L;
                    long unread = Math.max(0L, r.getLastMessageSeq() - lastRead);
                    return ChatRes.RoomListItem.from(r, unread);
                })
                .toList();

        String nextCursor = rooms.isEmpty() ? null : String.valueOf(rooms.get(rooms.size() - 1).getId());

        return new CursorPage<>(items, nextCursor, hasMore);
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

        return ChatRes.RoomDetail.from(room, resumeInfo);
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

        room.close();
    }

    /**
     * 메시지 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, Long roomId, ChatReq.ReadMessage request) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        Long lastReadSeq = request.lastReadSeq();
        Long lastMessageSeq = room.getLastMessageSeq();
        if (lastMessageSeq == null) {
            return;
        }
        if (lastReadSeq > lastMessageSeq) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        room.updateLastReadSeq(userId, lastReadSeq);
    }

    /**
     * 채팅방 메시지 목록 조회 (커서)
     */
    @Transactional
    public CursorPage<ChatRes.MessageInfo> getMessages(
            Long userId,
            Long roomId,
            Long cursorId,
            int size
    ) {
        // 권한 체크
        ChatRoom room = chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        List<ChatMessage> messages = chatMessageRepository.findByChatIdByCursor(
                roomId,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        if (room.getLastMessageSeq() != null) {
            // 메시지 페이지 커서와 무관하게, 방에 존재하는 최신 메시지까지 읽음 처리
            room.updateLastReadSeq(userId, room.getLastMessageSeq());
        }

        boolean hasMore = messages.size() > size;
        if (hasMore) {
            messages = messages.subList(0, size);
        }

        List<ChatRes.MessageInfo> items = messages.stream()
                .map(ChatRes.MessageInfo::from)
                .toList();

        String nextCursor = messages.isEmpty() ? null : String.valueOf(messages.get(messages.size() - 1).getId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }
}
