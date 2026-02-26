package org.refit.refitbackend.domain.expert.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.dto.ExpertReq;
import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.domain.expert.service.ExpertService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.expert.ExpertInternalSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/experts")
@Validated
@Tag(name = "Internal-Expert", description = "내부 연동용 현직자 API")
public class ExpertInternalController {

    private final ExpertService expertService;

    @ExpertInternalSwaggerSpec.UpdateEmbeddingInternal
    @PostMapping("/embeddings")
    public ResponseEntity<ApiResponse<Void>> updateEmbedding(
            @Valid @RequestBody ExpertReq.UpdateEmbedding request
    ) {
        expertService.updateEmbedding(request);
        return ResponseUtil.ok("success");
    }

    @ExpertInternalSwaggerSpec.RefreshMentorEmbeddingInternal
    @PutMapping("/embeddings/{user_id}")
    public ResponseEntity<ApiResponse<ExpertRes.MentorEmbeddingUpdateResponse>> refreshMentorEmbedding(
            @PathVariable("user_id") @Positive(message = "expert_user_id_invalid") Long userId
    ) {
        return ResponseUtil.ok("success", expertService.refreshMentorEmbedding(userId));
    }
}
