package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatFeedbackAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatFeedbackAnswerRepository extends JpaRepository<ChatFeedbackAnswer, Long> {

    @Query("""
            SELECT cfa FROM ChatFeedbackAnswer cfa
            JOIN FETCH cfa.question q
            WHERE cfa.chatFeedback.id = :chatFeedbackId
            ORDER BY q.displayOrder ASC, cfa.id ASC
            """)
    List<ChatFeedbackAnswer> findByChatFeedbackIdOrderByQuestion(@Param("chatFeedbackId") Long chatFeedbackId);
}
