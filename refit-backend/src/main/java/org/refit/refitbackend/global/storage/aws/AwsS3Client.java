package org.refit.refitbackend.global.storage.aws;

import java.net.URI;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.storage.StorageClient;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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

    private String createFileUrl(String filePath) {
        URI domain = URI.create(properties.domain());
        return domain.resolve(filePath).toString();
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
}
