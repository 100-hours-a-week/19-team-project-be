package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.service.ChatFeedbackService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.chat.ChatSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v2/chats/{chat_id}/feedback")
@Tag(name = "ChatV2", description = "채팅 V2 API")
public class ChatFeedbackV2Controller {

    private final ChatFeedbackService chatFeedbackService;

    @ChatSwaggerSpec.CreateChatFeedbackV2
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatFeedbackId>> createFeedback(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("chat_id") @Positive Long chatId,
            @Valid @RequestBody ChatReq.CreateFeedbackV2 request
    ) {
        return ResponseUtil.created("create_success",
                chatFeedbackService.createFeedback(principal.getUserId(), chatId, request));
    }

    @ChatSwaggerSpec.GetChatFeedbackV2
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatFeedbackDetail>> getFeedback(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("chat_id") @Positive Long chatId
    ) {
        return ResponseUtil.ok("success", chatFeedbackService.getFeedback(principal.getUserId(), chatId));
    }
}
