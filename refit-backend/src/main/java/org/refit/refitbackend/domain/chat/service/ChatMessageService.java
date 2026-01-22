package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.MessageType;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatRes.MessageInfo sendMessage(Long senderId, ChatReq.SendMessage request) {
        // 발신자 조회
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        // 채팅방 조회 및 권한 체크
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(request.chatId(), senderId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        long roomSequence = chatRoom.nextMessageSequence();

        // 메시지 타입 결정
        MessageType messageType = request.messageType() != null
                ? MessageType.valueOf(request.messageType())
                : MessageType.TEXT;

        // 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(messageType)
                .content(request.content())
                .roomSequence(roomSequence)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 채팅방의 마지막 메시지 업데이트
        chatRoom.updateLastMessage(savedMessage);

        return ChatRes.MessageInfo.from(savedMessage);
    }
}
