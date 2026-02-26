package org.refit.refitbackend.domain.jobposting.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class JobPostTaskReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateTask(
            @NotBlank(message = "요청 URL이 필요합니다.")
            String url,
            @NotBlank(message = "소스가 필요합니다.")
            String source
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompleteTask(
            @NotNull(message = "결과 데이터가 필요합니다.")
            @Valid
            ParsedData data
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ParsedData(
            @NotNull(message = "job_post_id가 필요합니다.")
            @Positive(message = "job_post_id가 필요합니다.")
            Long jobPostId,
            @NotBlank(message = "title이 필요합니다.")
            String title,
            @NotBlank(message = "company가 필요합니다.")
            String company,
            String department,
            String employmentType,
            String experienceRequired,
            String educationRequired,
            List<String> requirements,
            List<String> preferences,
            List<String> techStack,
            List<String> responsibilities
    ) {}
}
