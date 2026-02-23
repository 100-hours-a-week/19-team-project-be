package org.refit.refitbackend.domain.jobposting.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class JobPostTaskRes {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TaskResult(
            String taskId,
            String status,
            Long jobPostId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record JobPostSimple(
            Long jobPostId,
            String title,
            String company,
            String employmentType
    ) {}
}
