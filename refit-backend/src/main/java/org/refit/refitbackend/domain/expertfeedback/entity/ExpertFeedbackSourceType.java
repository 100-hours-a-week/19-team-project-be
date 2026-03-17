package org.refit.refitbackend.domain.expertfeedback.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ExpertFeedbackSourceType {
    SEED("seed"),
    REAL_MENTOR("real_mentor");

    private final String value;

    ExpertFeedbackSourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ExpertFeedbackSourceType from(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid source_type: " + value));
    }
}
