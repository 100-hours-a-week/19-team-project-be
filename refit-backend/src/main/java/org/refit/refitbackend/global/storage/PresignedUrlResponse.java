package org.refit.refitbackend.global.storage;

public record PresignedUrlResponse(
        String presignedUrl,
        String fileUrl
) {
}
