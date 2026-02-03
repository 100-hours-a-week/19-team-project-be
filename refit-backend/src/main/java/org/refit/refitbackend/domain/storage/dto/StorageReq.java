package org.refit.refitbackend.domain.storage.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StorageReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PresignedUrlRequest(
            @NotNull(message = "업로드 유형이 필요합니다.")
            UploadTarget targetType,
            @NotBlank(message = "파일 이름이 필요합니다.")
            String fileName,
            @NotNull(message = "파일 크기가 필요합니다.")
            Long fileSize
    ) {}

    public record PresignedDownloadRequest(
            @NotBlank(message = "파일 URL이 필요합니다.")
            String fileUrl
    ) {}

    public enum UploadTarget {
        PROFILE_IMAGE,
        RESUME_PDF
    }
}
