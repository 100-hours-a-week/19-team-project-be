package org.refit.refitbackend.domain.resume.dto;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public class ResumeRes {

    public record ResumeListItem(
            Long resumeId,
            String title,
            Boolean isFresher,
            String educationLevel,
            String fileUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ResumeDetail(
            Long resumeId,
            String title,
            Boolean isFresher,
            String educationLevel,
            String fileUrl,
            JsonNode contentJson,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ResumeListResponse(
            List<ResumeListItem> resumes
    ) {}

    public record ResumeId(
            Long resumeId
    ) {}
}
