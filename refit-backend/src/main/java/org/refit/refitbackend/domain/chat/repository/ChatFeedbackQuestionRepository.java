package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatFeedbackQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChatFeedbackQuestionRepository extends JpaRepository<ChatFeedbackQuestion, Long> {

    List<ChatFeedbackQuestion> findByIdInAndIsActiveTrue(Collection<Long> ids);
}
