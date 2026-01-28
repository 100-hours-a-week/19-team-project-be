package org.refit.refitbackend.domain.resume.dto;

import tools.jackson.databind.JsonNode;

public class ResumeTaskRes {

    public record ParsedResult(
            boolean isFresher,
            String educationLevel,
            JsonNode contentJson,
            String rawTextExcerpt
    ) {}

    public record TaskResult(
            String taskId,
            String status,
            ParsedResult result
    ) {}
}
