package org.refit.refitbackend.domain.storage.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.resume.entity.Resume;
import org.refit.refitbackend.domain.resume.repository.ResumeRepository;
import org.refit.refitbackend.domain.storage.entity.enums.DownloadTarget;
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

    private static final long MAX_RESUME_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PROFILE_IMAGE_BYTES = 10L * 1024 * 1024;

    private final StorageClient storageClient;
    private final ResumeRepository resumeRepository;
    private final ChatRoomRepository chatRoomRepository;

    public PresignedUrlResponse issuePresignedUrl(Long userId, StorageReq.PresignedUrlRequest request) {
        if (userId == null) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        validateUploadSize(request);
        String filePath = buildFilePath(userId, request);
        return storageClient.getPresignedUrl(filePath);
    }

    public PresignedUrlResponse issuePresignedDownloadUrl(Long userId, StorageReq.PresignedDownloadRequest request) {
        String fileUrl = request.fileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        if (userId == null) {
            throw new CustomException(ExceptionType.AUTH_UNAUTHORIZED);
        }

        DownloadTarget target = resolveDownloadTarget(fileUrl);
        switch (target) {
            case PROFILE_IMAGE -> {
                return storageClient.getPresignedDownloadUrl(fileUrl);
            }
            case RESUME_PDF -> {
                Resume resume = resumeRepository.findByFileUrl(fileUrl)
                        .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));
                Long ownerId = resume.getUser().getId();
                if (ownerId != null && ownerId.equals(userId)) {
                    return storageClient.getPresignedDownloadUrl(fileUrl);
                }
                boolean isParticipant = chatRoomRepository.existsByResumeIdAndUserId(resume.getId(), userId);
                if (isParticipant) {
                    return storageClient.getPresignedDownloadUrl(fileUrl);
                }
                throw new CustomException(ExceptionType.STORAGE_ACCESS_FORBIDDEN);
            }
            default -> throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
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

    private void validateUploadSize(StorageReq.PresignedUrlRequest request) {
        Long fileSize = request.fileSize();
        if (fileSize == null) {
            throw new CustomException(ExceptionType.FILE_SIZE_REQUIRED);
        }
        if (fileSize < 0) {
            throw new CustomException(ExceptionType.FILE_SIZE_INVALID);
        }
        if (request.targetType() == StorageReq.UploadTarget.RESUME_PDF && fileSize > MAX_RESUME_BYTES) {
            throw new CustomException(ExceptionType.RESUME_FILE_TOO_LARGE);
        }
        if (request.targetType() == StorageReq.UploadTarget.PROFILE_IMAGE
                && fileSize > MAX_PROFILE_IMAGE_BYTES) {
            throw new CustomException(ExceptionType.PROFILE_IMAGE_TOO_LARGE);
        }
    }

    private DownloadTarget resolveDownloadTarget(String fileUrl) {
        String normalized = fileUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("/profile-images/")) {
            return DownloadTarget.PROFILE_IMAGE;
        }
        if (normalized.contains("/resumes/original/")) {
            return DownloadTarget.RESUME_PDF;
        }
        return DownloadTarget.UNKNOWN;
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
