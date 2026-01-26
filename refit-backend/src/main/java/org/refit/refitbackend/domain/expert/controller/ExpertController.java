package org.refit.refitbackend.domain.expert.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.domain.expert.dto.ExpertReq;
import org.refit.refitbackend.domain.expert.service.ExpertService;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.expert.ExpertSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/experts")
@Validated
public class ExpertController {

    private final ExpertService expertService;

    @ExpertSwaggerSpec.SearchExperts
    @GetMapping
    public ResponseEntity<ApiResponse<ExpertRes.ExpertCursorResponse>> searchExperts(
            @Parameter(description = "검색 키워드")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "직무 ID")
            @RequestParam(name = "job_id", required = false) @Positive Long jobId,
            @Parameter(description = "스킬 ID")
            @RequestParam(name = "skill_id", required = false) @Positive Long skillId,
            @Parameter(description = "경력 레벨 ID")
            @RequestParam(name = "career_level", required = false) @Positive Long careerLevelId,
            @Parameter(description = "커서(마지막 유저 ID)")
            @RequestParam(required = false) @Positive Long cursor,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "5") @Positive int size
    ) {
        CursorPage<ExpertRes.ExpertListItem> page = expertService.searchExperts(
                keyword,
                jobId,
                skillId,
                careerLevelId,
                cursor,
                size
        );
        ExpertRes.ExpertCursorResponse res = new ExpertRes.ExpertCursorResponse(page.items(), page.nextCursor(), page.hasMore());
        return ResponseUtil.ok("success", res);
    }

    @ExpertSwaggerSpec.GetExpertDetail
    @GetMapping("/{user_id}")
    public ResponseEntity<ApiResponse<ExpertRes.ExpertDetail>> getExpertDetail(
            @PathVariable("user_id") @Positive(message = "expert_user_id_invalid") Long userId
    ) {
        return ResponseUtil.ok("success", expertService.getExpertDetail(userId));
    }

    @ExpertSwaggerSpec.UpdateEmbedding
    @PostMapping("/embeddings")
    public ResponseEntity<ApiResponse<Void>> updateEmbedding(
            @Valid @RequestBody ExpertReq.UpdateEmbedding request
    ) {
        expertService.updateEmbedding(request);
        return ResponseUtil.ok("success");
    }
}
