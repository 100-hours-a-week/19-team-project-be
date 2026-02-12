package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.service.ChatRequestService;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.chat.ChatSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v2/chats/requests")
@Tag(name = "ChatV2", description = "채팅 요청 V2 API")
public class ChatV2Controller {

    private final ChatRequestService chatRequestService;

    @ChatSwaggerSpec.CreateChatRequestV2
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatRequestId>> createRequest(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChatReq.CreateRequestV2 request
    ) {
        return ResponseUtil.created("create_success", chatRequestService.createRequest(principal.getUserId(), request));
    }

    @ChatSwaggerSpec.ListChatRequestsV2
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRes.ChatRequestCursorResponse>> getRequests(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "요청 방향", example = "received", required = true)
            @RequestParam @NotBlank String direction,
            @Parameter(description = "요청 상태 (PENDING, ACCEPTED, REJECTED)", example = "PENDING")
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "5") @Positive int size
    ) {
        CursorPage<ChatRes.ChatRequestItem> page;
        if ("received".equalsIgnoreCase(direction)) {
            page = chatRequestService.getReceivedRequests(principal.getUserId(), status, cursor, size);
        } else if ("sent".equalsIgnoreCase(direction)) {
            page = chatRequestService.getSentRequests(principal.getUserId(), status, cursor, size);
        } else {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        ChatRes.ChatRequestCursorResponse res = new ChatRes.ChatRequestCursorResponse(
                page.items(), page.nextCursor(), page.hasMore()
        );
        return ResponseUtil.ok("success", res);
    }

    @ChatSwaggerSpec.RespondChatRequestV2
    @PatchMapping("/{request_id}")
    public ResponseEntity<ApiResponse<ChatRes.RespondRequestResult>> respondRequest(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("request_id") @Positive Long requestId,
            @Valid @RequestBody ChatReq.RespondRequestV2 request
    ) {
        return ResponseUtil.ok("success", chatRequestService.respond(principal.getUserId(), requestId, request));
    }
}
