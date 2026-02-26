package org.refit.refitbackend.global.swagger.spec.report;

import org.refit.refitbackend.domain.report.dto.ReportReq;
import org.refit.refitbackend.domain.report.dto.ReportRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiConflictError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ReportSwaggerSpec {

    private ReportSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 리포트 생성",
            operationDescription = """
                    채팅방 기준으로 AI 리포트 생성을 요청합니다. (비동기 처리)
                    요청 성공 시 report_id를 즉시 반환하며, 리포트 상태는 초기 PROCESSING입니다.
                    실제 AI 리포트 생성은 Kafka consumer가 비동기로 수행하고 완료 시 reports 상태가 COMPLETED/FAILED로 변경됩니다.
                    리포트 상세 조회 API로 상태 및 결과를 확인할 수 있습니다.
                    """,
            responseCode = "201",
            responseDescription = "created",
            implementation = ReportRes.ReportId.class
    )
    @SwaggerApiRequestBody(
            implementation = ReportReq.Create.class,
            description = "리포트 생성 요청 (job_post_url은 chat_room에 저장된 값을 사용)",
            examples = {
                    "{ \"chat_room_id\": 3 }"
            },
            exampleNames = { "create_report" }
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.INVALID_REQUEST,
            ExceptionType.FEEDBACK_ANSWER_MISSING
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiNotFoundError(description = "chat_room_not_found", types = {
            ExceptionType.CHAT_ROOM_NOT_FOUND
    })
    @SwaggerApiConflictError(types = {
            ExceptionType.REPORT_ALREADY_EXISTS
    })
    @SwaggerApiError(responseCode = "500", description = "ai_server_error", types = {
            ExceptionType.AI_SERVER_ERROR
    })
    public @interface CreateReportV2 {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 내 리포트 목록 조회",
            operationDescription = "로그인 사용자 기준 리포트 목록을 최신순으로 조회합니다.",
            implementation = ReportRes.ReportListResponse.class
    )
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    public @interface ListReportsV2 {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 리포트 상세 조회",
            operationDescription = "리포트 상세 결과(JSON 포함)를 조회합니다. 비동기 생성 직후에는 PROCESSING 상태일 수 있습니다.",
            implementation = ReportRes.ReportDetail.class
    )
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiNotFoundError(description = "report_not_found", types = {
            ExceptionType.REPORT_NOT_FOUND
    })
    public @interface GetReportDetailV2 {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 리포트 삭제",
            operationDescription = "리포트를 삭제합니다. PROCESSING 상태는 삭제할 수 없습니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.REPORT_DELETE_NOT_ALLOWED
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiNotFoundError(description = "report_not_found", types = {
            ExceptionType.REPORT_NOT_FOUND
    })
    public @interface DeleteReportV2 {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 리포트 생성 재시도",
            operationDescription = "FAILED 상태 리포트에 대해서만 AI 생성을 재시도합니다.",
            implementation = ApiResponse.class
    )
    @SwaggerApiBadRequestError(types = {
            ExceptionType.REPORT_STATUS_NOT_FAILED
    })
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiNotFoundError(description = "report_not_found", types = {
            ExceptionType.REPORT_NOT_FOUND,
            ExceptionType.CHAT_ROOM_NOT_FOUND
    })
    @SwaggerApiConflictError(types = {
            ExceptionType.REPORT_ALREADY_PROCESSING
    })
    @SwaggerApiError(responseCode = "500", description = "ai_server_error", types = {
            ExceptionType.AI_SERVER_ERROR
    })
    public @interface RetryReportV2 {}
}
