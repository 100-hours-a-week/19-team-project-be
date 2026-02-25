package org.refit.refitbackend.domain.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.global.sse.SseService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/events")
@Tag(name = "SSE", description = "실시간 이벤트 구독 API")
public class EventStreamController {

    private final SseService sseService;

    @Operation(
            summary = "SSE 구독 연결",
            description = "채팅/알림 이벤트 수신을 위한 SSE 연결을 생성합니다. JWT 인증이 필요합니다."
    )
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails principal) {
        return sseService.subscribe(principal.getUserId());
    }
}
