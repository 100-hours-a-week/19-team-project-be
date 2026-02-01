package org.refit.refitbackend.domain.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public class ResumeReq {

    public record Create(
            @NotBlank(message = "이력서 제목을 입력해 주세요.")
            @Size(max = 30, message = "이력서 제목이 너무 깁니다.")
            String title,

            @NotNull(message = "신입 여부 값이 올바르지 않습니다.")
            Boolean isFresher,

            @NotBlank(message = "학력 값이 올바르지 않습니다.")
            String educationLevel,

            String fileUrl,

            @NotNull(message = "이력서 내용이 올바르지 않습니다.")
            JsonNode contentJson
    ) {}

    public record Update(
            @Size(max = 30, message = "이력서 제목이 너무 깁니다.")
            String title,
            Boolean isFresher,
            String educationLevel,
            String fileUrl,
            JsonNode contentJson
    ) {}

    public record UpdateTitle(
            @NotBlank(message = "이력서 제목을 입력해 주세요.")
            @Size(max = 30, message = "이력서 제목이 너무 깁니다.")
            String title
    ) {}
}
