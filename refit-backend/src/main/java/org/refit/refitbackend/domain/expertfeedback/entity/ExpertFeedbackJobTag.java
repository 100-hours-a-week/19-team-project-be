package org.refit.refitbackend.domain.expertfeedback.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ExpertFeedbackJobTag {
    AI("AI"),
    BE("BE"),
    FE("FE"),
    DATA("DATA"),
    COMMON("common");

    private final String value;

    ExpertFeedbackJobTag(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ExpertFeedbackJobTag from(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid job_tag: " + value));
    }
}
