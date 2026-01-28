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
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    @SwaggerApiRequestBody(
            implementation = StorageReq.PresignedUrlRequest.class,
            examples = {
                    "{ \"target_type\": \"PROFILE_IMAGE\", \"file_name\": \"profile.jpg\" }",
                    "{ \"target_type\": \"RESUME_PDF\", \"file_name\": \"resume.pdf\" }"
            },
            exampleNames = {
                    "profile_image",
                    "resume_pdf"
            }
    )
    public @interface IssuePresignedUrl {}
}
