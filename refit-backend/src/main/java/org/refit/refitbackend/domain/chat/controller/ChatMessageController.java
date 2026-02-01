package org.refit.refitbackend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    /**
     * 메시지 전송 (WebSocket)
     * 클라이언트: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload ChatReq.SendMessage request,
            Principal principal
    ) {
        try {
            // 인증된 사용자 ID 추출 (JWT에서)
            Long senderId = Long.parseLong(principal.getName());

            // 메시지 저장
            chatMessageService.sendMessage(senderId, request);


        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
        }
    }
}
