package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.service.ChatReviewService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.chat.ChatSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v3/chats/{chat_id}/reviews")
@Tag(name = "ChatV3", description = "채팅 V3 API")
public class ChatReviewV3Controller {

    private final ChatReviewService chatReviewService;

    @ChatSwaggerSpec.CreateChatReviewV3
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatReviewDetail>> createReview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("chat_id") @Positive Long chatId,
            @Valid @RequestBody ChatReq.CreateReviewV3 request
    ) {
        return ResponseUtil.created("create_success",
                chatReviewService.createReview(principal.getUserId(), chatId, request));
    }

    @ChatSwaggerSpec.UpdateChatReviewV3
    @PatchMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatReviewDetail>> updateReview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("chat_id") @Positive Long chatId,
            @Valid @RequestBody ChatReq.CreateReviewV3 request
    ) {
        return ResponseUtil.ok("success",
                chatReviewService.updateReview(principal.getUserId(), chatId, request));
    }

    @ChatSwaggerSpec.DeleteChatReviewV3
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("chat_id") @Positive Long chatId
    ) {
        chatReviewService.deleteReview(principal.getUserId(), chatId);
        return ResponseUtil.ok("success");
    }
}
