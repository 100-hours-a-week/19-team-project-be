package org.refit.refitbackend.domain.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.chat.ChatSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/docs/ws")
@Tag(name = "WebSocket", description = "WebSocket (STOMP) 메시지 API 문서")
public class ChatWebSocketDocController {

    @ChatSwaggerSpec.WebSocketSendMessageDoc
    @PostMapping("/chat/send")
    public ResponseEntity<ApiResponse<ChatRes.MessageInfo>> sendMessageDoc(
            @RequestBody ChatReq.SendMessage request
    ) {
        // Documentation-only endpoint (does not send a message).
        return ResponseUtil.created("created_success", null);
    }
}
