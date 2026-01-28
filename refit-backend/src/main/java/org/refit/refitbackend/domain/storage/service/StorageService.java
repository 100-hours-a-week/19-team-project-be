package org.refit.refitbackend.domain.storage.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.storage.dto.StorageReq;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.storage.StorageClient;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageClient storageClient;

    public PresignedUrlResponse issuePresignedUrl(Long userId, StorageReq.PresignedUrlRequest request) {
        if (userId == null) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        String filePath = buildFilePath(userId, request);
        return storageClient.getPresignedUrl(filePath);
    }

    private String buildFilePath(Long userId, StorageReq.PresignedUrlRequest request) {
        StorageReq.UploadTarget targetType = request.targetType();
        String fileName = request.fileName();
        String extension = extractExtension(fileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        return switch (targetType) {
            case PROFILE_IMAGE -> String.format(
                    Locale.ROOT,
                    "profile-images/%d/%s%s",
                    userId,
                    uuid,
                    extension
            );
            case RESUME_PDF -> String.format(
                    Locale.ROOT,
                    "resumes/original/%d/%s%s",
                    userId,
                    uuid,
                    extension
            );
        };
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        String trimmed = fileName.trim();
        if (trimmed.isEmpty()) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == trimmed.length() - 1) {
            return "";
        }
        String ext = trimmed.substring(dotIndex).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]+") ? ext : "";
    }
}
