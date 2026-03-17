package org.refit.refitbackend.domain.expertfeedback.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expertfeedback.dto.ExpertFeedbackReq;
import org.refit.refitbackend.domain.expertfeedback.dto.ExpertFeedbackRes;
import org.refit.refitbackend.domain.expertfeedback.service.ExpertFeedbackService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.expert.ExpertFeedbackInternalSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/internal/expert-feedbacks")
@Tag(name = "Internal-ExpertFeedback", description = "내부 연동용 멘토 Q&A RAG 저장 API")
public class ExpertFeedbackController {

    private final ExpertFeedbackService expertFeedbackService;

    @ExpertFeedbackInternalSwaggerSpec.CreateBatchInternal
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<ExpertFeedbackRes.BatchInsertResult>> createBatch(
            @Valid @RequestBody ExpertFeedbackReq.BatchCreateFeedback request
    ) {
        return ResponseUtil.created("success", expertFeedbackService.createBatch(request));
    }

    @ExpertFeedbackInternalSwaggerSpec.CreateInternal
    @PostMapping
    public ResponseEntity<ApiResponse<ExpertFeedbackRes.CreatedId>> create(
            @Valid @RequestBody ExpertFeedbackReq.CreateFeedback request
    ) {
        return ResponseUtil.created("success", expertFeedbackService.create(request));
    }
}
