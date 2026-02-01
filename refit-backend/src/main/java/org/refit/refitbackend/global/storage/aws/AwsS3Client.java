package org.refit.refitbackend.global.storage.aws;

import java.net.URI;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.storage.StorageClient;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class AwsS3Client implements StorageClient {
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;
    private final AwsS3ClientProperties properties;

    @Override
    public PresignedUrlResponse getPresignedUrl(String filePath) {
        PutObjectPresignRequest presignRequest = buildPresignedRequest(filePath);
        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = createFileUrl(filePath);
        return new PresignedUrlResponse(presignedUrl, fileUrl);
    }

    @Override
    public PresignedUrlResponse getPresignedDownloadUrl(String fileUrl) {
        String filePath = extractFilePath(fileUrl);
        GetObjectPresignRequest presignRequest = buildPresignedGetRequest(filePath);
        String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
        return new PresignedUrlResponse(presignedUrl, createFileUrl(filePath));
    }

    private String createFileUrl(String filePath) {
        URI domain = URI.create(properties.domain());
        return domain.resolve(filePath).toString();
    }

    private String extractFilePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("fileUrl is required");
        }
        String domain = properties.domain();
        if (domain == null || domain.isBlank()) {
            throw new IllegalArgumentException("storage domain is required");
        }
        if (!domain.endsWith("/")) {
            domain = domain + "/";
        }
        if (!fileUrl.startsWith(domain)) {
            throw new IllegalArgumentException("invalid fileUrl");
        }
        return fileUrl.substring(domain.length());
    }

    private PutObjectPresignRequest buildPresignedRequest(String filePath) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(filePath);
        return PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .putObjectRequest(requestBuilder.build())
                .build();
    }

    private GetObjectPresignRequest buildPresignedGetRequest(String filePath) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(filePath)
                .build();
        return GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .getObjectRequest(request)
                .build();
    }
}
