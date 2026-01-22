package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

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

        return ChatRes.RoomDetail.from(room);
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
    public void markAsRead(Long userId, ChatReq.ReadMessage request) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(request.roomId(), userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        ChatMessage message = chatMessageRepository.findById(request.messageId())
                .orElseThrow(() -> new CustomException(ExceptionType.MESSAGE_NOT_FOUND));

        room.updateLastReadMessage(userId, message);
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
        // 권한 체크
        ChatRoom room = chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdByCursor(
                roomId,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        if (!messages.isEmpty()) {
            ChatMessage latest = messages.get(0); // id DESC 기준
            room.updateLastReadMessage(userId, latest);
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
