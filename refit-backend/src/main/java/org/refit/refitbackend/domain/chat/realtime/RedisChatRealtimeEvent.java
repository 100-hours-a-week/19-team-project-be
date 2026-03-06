package org.refit.refitbackend.domain.chat.realtime;

import org.refit.refitbackend.domain.chat.dto.ChatRes;

public record RedisChatRealtimeEvent(
        Long chatId,
        ChatRes.MessageInfo payload
) {
}

