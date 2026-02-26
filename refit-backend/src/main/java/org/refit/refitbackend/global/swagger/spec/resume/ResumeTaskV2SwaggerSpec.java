package org.refit.refitbackend.global.swagger.spec.resume;

import org.refit.refitbackend.domain.resume.dto.ResumeTaskReq;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ResumeTaskV2SwaggerSpec {

    private ResumeTaskV2SwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 이력서 파싱 비동기 작업 생성",
            operationDescription = """
                    이력서 파싱 비동기 작업(task)을 생성합니다.
                    응답으로 task_id와 PROCESSING 상태를 즉시 반환하며, 실제 파싱은 Kafka consumer가 비동기로 처리합니다.
                    파싱 결과는 'V2 이력서 파싱 작업 조회' API에서 확인할 수 있습니다.
                    """,
            responseCode = "201",
            responseDescription = "task_created",
            implementation = ResumeTaskRes.TaskResult.class
    )
    @SwaggerApiRequestBody(
            implementation = ResumeTaskReq.CreateTask.class,
            description = "비동기 이력서 파싱 작업 생성 요청 (파싱할 PDF 파일 URL)",
            examples = {
                    "{ \"file_url\": \"https://refit-storage-dev.s3.ap-northeast-2.amazonaws.com/resumes/original/example.pdf\" }"
            },
            exampleNames = { "create_resume_parse_task" }
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_FILE_URL_INVALID,
            ExceptionType.RESUME_FILE_NOT_PDF
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "500", description = "internal_server_error", types = {
            ExceptionType.INTERNAL_SERVER_ERROR
    })
    public @interface CreateParseTaskV2 {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 이력서 파싱 작업 조회",
            operationDescription = """
                    이력서 파싱 작업 상태를 조회합니다.
                    status는 PROCESSING / COMPLETED / FAILED 중 하나이며,
                    COMPLETED인 경우 result에 파싱 결과가 포함됩니다.
                    """,
            implementation = ResumeTaskRes.TaskResult.class
    )
    @SwaggerApiNotFoundError(description = "task_not_found", types = {
            ExceptionType.TASK_NOT_FOUND
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "403", description = "forbidden", types = {
            ExceptionType.FORBIDDEN
    })
    public @interface GetParseTaskV2 {}
}
