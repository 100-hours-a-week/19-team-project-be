package org.refit.refitbackend.global.storage;

public interface StorageClient {
    PresignedUrlResponse getPresignedUrl(String filePath);
}
