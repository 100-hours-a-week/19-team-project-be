package org.refit.refitbackend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송 (WebSocket)
     * 클라이언트: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload ChatReq.SendMessage request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            // 인증된 사용자 ID 추출 (JWT에서)
            Long senderId = Long.parseLong(principal.getName());

            // 메시지 저장
            ChatRes.MessageInfo messageInfo = chatMessageService.sendMessage(senderId, request);

            // 해당 채팅방 구독자들에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/queue/chat." + request.chatId(),
                    messageInfo
            );

            log.info("메시지 전송 성공 - roomId: {}, senderId: {}", request.chatId(), senderId);

        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
            // 에러 메시지를 발신자에게만 전송
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    "message_send_failed: " + e.getMessage()
            );
        }
    }
}
