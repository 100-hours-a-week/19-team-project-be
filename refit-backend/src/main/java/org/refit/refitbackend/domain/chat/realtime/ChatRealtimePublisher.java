package org.refit.refitbackend.domain.chat.realtime;

import org.refit.refitbackend.domain.chat.dto.ChatRes;

public interface ChatRealtimePublisher {

    void publish(Long chatId, ChatRes.MessageInfo payload);
}

