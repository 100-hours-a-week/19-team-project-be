package org.refit.refitbackend.global.swagger.spec.jobposting;

import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskReq;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class JobPostTaskV2SwaggerSpec {

    private JobPostTaskV2SwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "V2 공고 크롤링 가능 여부 검증",
            operationDescription = """
                    입력한 공고 URL을 실제 크롤링 API(/repo/job-post)로 사전 검증합니다.
                    - crawlable=true: 파싱 가능 (title/company 또는 핵심 필드 확인)
                    - crawlable=false: 파싱 실패/빈 결과
                    """,
            implementation = JobPostTaskRes.CrawlValidation.class
    )
    @SwaggerApiRequestBody(
            implementation = JobPostTaskReq.ValidateCrawl.class,
            description = "크롤링 검증 요청",
            examples = {
                    "{ \"url\": \"https://www.jobkorea.co.kr/Recruit/GI_Read/48665598?PageGbn=ST&TpGb=TP\" }"
            },
            exampleNames = { "validate_job_post_crawl" }
    )
    @SwaggerApiUnauthorizedError(types = {
            ExceptionType.AUTH_UNAUTHORIZED,
            ExceptionType.AUTH_INVALID_TOKEN,
            ExceptionType.AUTH_TOKEN_EXPIRED
    })
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiError(responseCode = "422", description = "job_post_parse_failed", types = {
            ExceptionType.JOB_POST_PARSE_FAILED
    })
    @SwaggerApiError(responseCode = "500", description = "ai_server_error", types = {
            ExceptionType.AI_SERVER_ERROR
    })
    public @interface ValidateCrawlV2 {}
}
