package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
          SELECT cm FROM ChatMessage cm
          WHERE cm.chatRoom.id = :chatId
          ORDER BY cm.roomSequence ASC
      """)
    java.util.List<ChatMessage> findAllByChatIdOrderBySequence(@Param("chatId") Long chatId);

}
