package org.refit.refitbackend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(name = "chat_feedback_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatFeedbackAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_feedback_id", nullable = false)
    private ChatFeedback chatFeedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ChatFeedbackQuestion question;

    @Column(name = "answer_value", nullable = false, columnDefinition = "TEXT")
    private String answerValue;

    @Builder
    private ChatFeedbackAnswer(ChatFeedback chatFeedback, ChatFeedbackQuestion question, String answerValue) {
        this.chatFeedback = chatFeedback;
        this.question = question;
        this.answerValue = answerValue;
    }
}
