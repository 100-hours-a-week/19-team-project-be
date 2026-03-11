package org.refit.refitbackend.domain.agent.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.agent.dto.AgentReq;
import org.refit.refitbackend.domain.agent.dto.AgentRes;
import org.refit.refitbackend.domain.agent.service.AgentService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.agent.AgentSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/agent")
@Tag(name = "Agent", description = "AI Agent API")
public class AgentController {

    private final AgentService agentService;

    @AgentSwaggerSpec.CreateSession
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<AgentRes.SessionInfo>> createSession(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        AgentRes.SessionInfo info = agentService.createSession(principal.getUserId());
        return ResponseUtil.ok("success", info);
    }

    @AgentSwaggerSpec.ListSessions
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<AgentRes.SessionInfo>>> listSessions(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<AgentRes.SessionInfo> items = agentService.listSessions(principal.getUserId());
        return ResponseUtil.ok("success", items);
    }

    @AgentSwaggerSpec.GetSession
    @GetMapping("/sessions/{session_id}")
    public ResponseEntity<ApiResponse<AgentRes.SessionInfo>> getSession(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("session_id") String sessionId
    ) {
        AgentRes.SessionInfo info = agentService.getSession(principal.getUserId(), sessionId);
        return ResponseUtil.ok("success", info);
    }

    @AgentSwaggerSpec.GetMessages
    @GetMapping("/sessions/{session_id}/messages")
    public ResponseEntity<ApiResponse<AgentRes.MessageList>> getMessages(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("session_id") String sessionId
    ) {
        List<AgentRes.MessageInfo> messages = agentService.getMessages(principal.getUserId(), sessionId);
        return ResponseUtil.ok("success", new AgentRes.MessageList(messages));
    }

    @AgentSwaggerSpec.ReplyStream
    @PostMapping(value = "/reply", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reply(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AgentReq.ReplyRequest request
    ) {
        return agentService.replyStream(principal.getUserId(), request);
    }
}
