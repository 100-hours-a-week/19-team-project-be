package org.refit.refitbackend.domain.expertfeedback.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ExpertFeedbackQuestionType {
    RESUME_FEEDBACK("resume_feedback"),
    INTERVIEW_PREP("interview_prep"),
    PROJECT_IMPROVEMENT("project_improvement"),
    JOB_FIT("job_fit"),
    CAREER_ADVICE("career_advice"),
    TECH_STACK("tech_stack");

    private final String value;

    ExpertFeedbackQuestionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ExpertFeedbackQuestionType from(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid question_type: " + value));
    }
}
