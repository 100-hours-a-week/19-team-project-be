package org.refit.refitbackend.domain.resume.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResumeReq {

    public record Create(
            @NotBlank(message = "resume_title_empty")
            @Size(max = 30, message = "resume_title_too_long")
            String title,

            @NotNull(message = "resume_is_fresher_invalid")
            Boolean isFresher,

            @NotBlank(message = "resume_education_level_invalid")
            String educationLevel,

            String fileUrl,

            @NotNull(message = "resume_content_invalid")
            JsonNode contentJson
    ) {}

    public record Update(
            @Size(max = 30, message = "resume_title_too_long")
            String title,
            Boolean isFresher,
            String educationLevel,
            String fileUrl,
            JsonNode contentJson
    ) {}

    public record UpdateTitle(
            @NotBlank(message = "resume_title_empty")
            @Size(max = 30, message = "resume_title_too_long")
            String title
    ) {}
}
