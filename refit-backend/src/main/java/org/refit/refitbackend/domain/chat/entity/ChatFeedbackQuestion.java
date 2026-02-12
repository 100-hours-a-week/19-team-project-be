package org.refit.refitbackend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(name = "chat_feedback_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatFeedbackQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", nullable = false, length = 50)
    private String questionKey;

    @Column(name = "question_text", nullable = false, length = 255)
    private String questionText;

    @Column(name = "answer_type", nullable = false, length = 20)
    private String answerType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
