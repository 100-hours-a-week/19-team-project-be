package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.repository.projection.ChatMessageCursorProjection;
import org.refit.refitbackend.domain.chat.repository.projection.ReportChatMessageProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // (CURSOR) 채팅방 메시지 목록 조회 (최신순)
    @Query("""
          SELECT cm FROM ChatMessage cm
          WHERE cm.chatRoom.id = :chatId
          AND (:cursorId IS NULL OR cm.id < :cursorId)
          ORDER BY cm.id DESC
      """)
    java.util.List<ChatMessage> findByChatIdByCursor(
            @Param("chatId") Long chatId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
          SELECT
            cm.id AS messageId,
            cm.chatRoom.id AS chatId,
            cm.roomSequence AS roomSequence,
            cm.sender.id AS senderId,
            cm.sender.nickname AS senderNickname,
            cm.sender.profileImageUrl AS senderProfileImageUrl,
            cm.sender.userType AS senderUserType,
            cm.messageType AS messageType,
            cm.content AS content,
            cm.clientMessageId AS clientMessageId,
            cm.createdAt AS createdAt
          FROM ChatMessage cm
          WHERE cm.chatRoom.id = :chatId
            AND (:cursorId IS NULL OR cm.id < :cursorId)
          ORDER BY cm.id DESC
      """)
    List<ChatMessageCursorProjection> findMessageSummariesByChatIdByCursor(
            @Param("chatId") Long chatId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
          SELECT
            cm.id AS messageId,
            cm.chatRoom.id AS chatId,
            cm.roomSequence AS roomSequence,
            cm.sender.id AS senderId,
            cm.sender.nickname AS senderNickname,
            cm.sender.profileImageUrl AS senderProfileImageUrl,
            cm.sender.userType AS senderUserType,
            cm.messageType AS messageType,
            cm.content AS content,
            cm.clientMessageId AS clientMessageId,
            cm.createdAt AS createdAt
          FROM ChatMessage cm
          JOIN cm.chatRoom cr
          WHERE cr.id = :chatId
            AND (cr.requester.id = :userId OR cr.receiver.id = :userId)
            AND (:cursorId IS NULL OR cm.id < :cursorId)
          ORDER BY cm.id DESC
      """)
    List<ChatMessageCursorProjection> findMessageSummariesByChatIdAndUserIdByCursor(
            @Param("chatId") Long chatId,
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
          SELECT cm FROM ChatMessage cm
          WHERE cm.chatRoom.id = :chatId
          ORDER BY cm.roomSequence ASC
      """)
    java.util.List<ChatMessage> findAllByChatIdOrderBySequence(@Param("chatId") Long chatId);

    @Query("""
          SELECT
            cm.id AS messageId,
            cm.chatRoom.id AS chatId,
            cm.roomSequence AS roomSequence,
            cm.sender.id AS senderId,
            cm.sender.nickname AS senderNickname,
            cm.sender.profileImageUrl AS senderProfileImageUrl,
            cm.sender.userType AS senderUserType,
            cm.messageType AS messageType,
            cm.content AS content,
            cm.clientMessageId AS clientMessageId,
            cm.createdAt AS createdAt
          FROM ChatMessage cm
          WHERE cm.chatRoom.id = :chatId
          ORDER BY cm.roomSequence ASC
      """)
    List<ReportChatMessageProjection> findReportMessagesByChatIdOrderBySequence(@Param("chatId") Long chatId);

    List<ChatMessage> findAllByChatRoom_IdAndClientMessageIdIn(Long chatRoomId, Collection<String> clientMessageIds);

    List<ChatMessage> findAllByChatRoom_IdAndRoomSequenceIn(Long chatRoomId, Collection<Long> roomSequences);

}
