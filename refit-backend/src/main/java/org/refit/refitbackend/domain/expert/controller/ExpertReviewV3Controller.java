package org.refit.refitbackend.domain.expert.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.service.ChatReviewService;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.expert.ExpertSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v3/experts/{user_id}/reviews")
public class ExpertReviewV3Controller {

    private final ChatReviewService chatReviewService;

    @ExpertSwaggerSpec.GetExpertReviewsV3
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatReviewCursorResponse>> getExpertReviews(
            @PathVariable("user_id") @Positive Long userId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "10") @Positive int size
    ) {
        CursorPage<ChatRes.ChatReviewItem> page = chatReviewService.getReviewsByExpertId(userId, cursor, size);
        ChatRes.ChatReviewCursorResponse response = new ChatRes.ChatReviewCursorResponse(
                page.items(), page.nextCursor(), page.hasMore()
        );
        return ResponseUtil.ok("success", response);
    }
}
