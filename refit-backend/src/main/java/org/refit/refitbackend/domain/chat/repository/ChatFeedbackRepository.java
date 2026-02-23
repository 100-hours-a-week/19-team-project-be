package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {

    boolean existsByChatRoomId(Long chatRoomId);

    @Query("""
            SELECT cf FROM ChatFeedback cf
            JOIN FETCH cf.chatRoom cr
            JOIN FETCH cf.expert e
            JOIN FETCH cf.user u
            WHERE cr.id = :chatRoomId
            """)
    Optional<ChatFeedback> findDetailByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
