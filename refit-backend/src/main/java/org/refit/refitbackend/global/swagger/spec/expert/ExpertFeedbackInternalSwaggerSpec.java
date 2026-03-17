package org.refit.refitbackend.global.swagger.spec.expert;

import org.refit.refitbackend.domain.expertfeedback.dto.ExpertFeedbackReq;
import org.refit.refitbackend.domain.expertfeedback.dto.ExpertFeedbackRes;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ExpertFeedbackInternalSwaggerSpec {

    private ExpertFeedbackInternalSwaggerSpec() {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Internal 멘토 Q&A 피드백 일괄 저장",
            operationDescription = "AI 서버가 시드 데이터 또는 멘토 Q&A 데이터를 내부 전용 API로 일괄 저장합니다.",
            implementation = ExpertFeedbackRes.BatchInsertResult.class
    )
    @SwaggerApiRequestBody(
            implementation = ExpertFeedbackReq.BatchCreateFeedback.class,
            examples = {
                    """
                    {
                      "feedbacks": [
                        {
                          "mentor_id": 0,
                          "question": "RAG 프로젝트 경험은 있는데...",
                          "answer": "RAG 시스템의 성능을 어떻게 평가했는지...",
                          "job_tag": "AI",
                          "question_type": "resume_feedback",
                          "embedding_text": "RAG 운영 경험 부족 보완...",
                          "source_type": "seed",
                          "quality_score": 5,
                          "embedding": [0.023, 0.004]
                        }
                      ]
                    }
                    """
            },
            exampleNames = { "batch_create_feedbacks" }
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    public @interface CreateBatchInternal {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "Internal 멘토 Q&A 피드백 단건 저장",
            operationDescription = "AI 서버 또는 백엔드가 멘토 Q&A 피드백 데이터를 내부 전용 API로 단건 저장합니다.",
            implementation = ExpertFeedbackRes.CreatedId.class
    )
    @SwaggerApiRequestBody(
            implementation = ExpertFeedbackReq.CreateFeedback.class,
            examples = {
                    """
                    {
                      "mentor_id": 101,
                      "question": "백엔드 신입으로 취업하고 싶은데 어떤 프로젝트가 경쟁력 있을까요?",
                      "answer": "단순한 CRUD보다는 대용량 트래픽 처리를 고려한 아키텍처...",
                      "job_tag": "BE",
                      "question_type": "project_improvement",
                      "embedding_text": "백엔드 신입 프로젝트 경쟁력, 대용량 트래픽...",
                      "source_type": "real_mentor",
                      "quality_score": 5,
                      "embedding": [0.023, 0.004]
                    }
                    """
            },
            exampleNames = { "create_feedback" }
    )
    @SwaggerApiError(responseCode = "400", description = "invalid_request", types = {
            ExceptionType.INVALID_REQUEST
    })
    @SwaggerApiError(responseCode = "401", description = "unauthorized", types = {
            ExceptionType.AUTH_UNAUTHORIZED
    })
    public @interface CreateInternal {}
}
