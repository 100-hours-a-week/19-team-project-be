package org.refit.refitbackend.global.swagger.spec.resume;

import org.refit.refitbackend.domain.resume.dto.ResumeReq;
import org.refit.refitbackend.domain.resume.dto.ResumeRes;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskReq;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiConflictError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ResumeSwaggerSpec {

    private ResumeSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이력서 생성",
            operationDescription = "이력서를 생성합니다.",
            responseCode = "201",
            implementation = ResumeRes.ResumeId.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_TITLE_EMPTY,
            ExceptionType.RESUME_TITLE_TOO_LONG,
            ExceptionType.RESUME_IS_FRESHER_INVALID,
            ExceptionType.RESUME_EDUCATION_LEVEL_INVALID,
            ExceptionType.RESUME_CONTENT_INVALID,
            ExceptionType.INVALID_JSON
    })
    @SwaggerApiConflictError(types = {
            ExceptionType.RESUME_LIMIT_EXCEEDED
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = ResumeReq.Create.class,
            examples = {
                    "{ \"title\": \"백엔드 이력서\", \"is_fresher\": true, \"education_level\": \"대졸\", \"file_url\": null, \"content_json\": {\"summary\": \"...\"} }"
            },
            exampleNames = { "create_resume" }
    )
    public @interface CreateResume {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "내 이력서 목록 조회",
            operationDescription = "로그인 사용자의 이력서 목록을 조회합니다.",
            implementation = ResumeRes.ResumeListResponse.class
    )
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface ListResumes {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이력서 상세 조회",
            operationDescription = "이력서 상세를 조회합니다.",
            implementation = ResumeRes.ResumeDetail.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_ID_INVALID
    })
    @SwaggerApiNotFoundError(description = "resume_not_found", types = {
            ExceptionType.RESUME_NOT_FOUND
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface GetResumeDetail {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이력서 수정",
            operationDescription = "이력서를 수정합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_ID_INVALID,
            ExceptionType.RESUME_TITLE_TOO_LONG,
            ExceptionType.INVALID_JSON
    })
    @SwaggerApiNotFoundError(description = "resume_not_found", types = {
            ExceptionType.RESUME_NOT_FOUND
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = ResumeReq.Update.class,
            examples = {
                    "{ \"title\": \"백엔드 이력서 수정\", \"content_json\": {\"summary\": \"...\"} }"
            },
            exampleNames = { "update_resume" }
    )
    public @interface UpdateResume {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이력서 제목 수정",
            operationDescription = "이력서 제목만 수정합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_ID_INVALID,
            ExceptionType.RESUME_TITLE_EMPTY,
            ExceptionType.RESUME_TITLE_TOO_LONG
    })
    @SwaggerApiNotFoundError(description = "resume_not_found", types = {
            ExceptionType.RESUME_NOT_FOUND
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    @SwaggerApiRequestBody(
            implementation = ResumeReq.UpdateTitle.class,
            examples = {
                    "{ \"title\": \"이력서 제목 수정\" }"
            },
            exampleNames = { "update_resume_title" }
    )
    public @interface UpdateResumeTitle {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이력서 삭제",
            operationDescription = "이력서를 삭제합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_ID_INVALID
    })
    @SwaggerApiNotFoundError(description = "resume_not_found", types = {
            ExceptionType.RESUME_NOT_FOUND
    })
    @SwaggerApiUnauthorizedError(types = { ExceptionType.AUTH_UNAUTHORIZED })
    public @interface DeleteResume {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이력서 파싱(동기)",
            operationDescription = "AI 서버로 이력서 파싱을 요청합니다. mode=async는 지원하지 않습니다.",
            implementation = ResumeTaskRes.TaskResult.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.RESUME_FILE_URL_INVALID,
            ExceptionType.RESUME_FILE_NOT_PDF,
            ExceptionType.RESUME_MODE_INVALID
    })
    @SwaggerApiRequestBody(
            implementation = ResumeTaskReq.Parse.class,
            examples = {
                    "{ \"file_url\": \"https://cdn.example.com/resume.pdf\", \"mode\": \"sync\" }"
            },
            exampleNames = { "resume_parse_sync" }
    )
    public @interface ParseResumeTask {}
}
