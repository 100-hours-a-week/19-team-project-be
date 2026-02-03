package org.refit.refitbackend.global.swagger.spec.storage;

import org.refit.refitbackend.domain.storage.dto.StorageReq;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class StorageSwaggerSpec {

    private StorageSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Presigned URL 발급",
            operationDescription = "S3 업로드용 presigned URL을 발급합니다.",
            implementation = PresignedUrlResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_REQUEST,
            ExceptionType.RESUME_FILE_TOO_LARGE
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    @SwaggerApiRequestBody(
            implementation = StorageReq.PresignedUrlRequest.class,
            examples = {
                    "{ \"target_type\": \"PROFILE_IMAGE\", \"file_name\": \"profile.jpg\", \"file_size\": 123456 }",
                    "{ \"target_type\": \"RESUME_PDF\", \"file_name\": \"resume.pdf\", \"file_size\": 5242880 }"
            },
            exampleNames = {
                    "profile_image",
                    "resume_pdf"
            }
    )
    public @interface IssuePresignedUrl {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Presigned URL 발급 (다운로드, POST)",
            operationDescription = "S3 다운로드용 presigned URL을 발급합니다. (POST)",
            implementation = PresignedUrlResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    @SwaggerApiRequestBody(
            implementation = StorageReq.PresignedDownloadRequest.class,
            examples = {
                    "{ \"file_url\": \"https://refit-storage-prod.s3.ap-northeast-2.amazonaws.com/resumes/original/1/abc123.pdf\" }"
            },
            exampleNames = {
                    "download_url"
            }
    )
    public @interface IssuePresignedDownloadUrlPost {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Presigned URL 발급 (다운로드, GET)",
            operationDescription = "S3 다운로드용 presigned URL을 발급합니다. (GET)",
            implementation = PresignedUrlResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    public @interface IssuePresignedDownloadUrlGet {}
}
