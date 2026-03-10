package org.refit.refitbackend.domain.chat.repository.projection;

import org.refit.refitbackend.domain.chat.entity.MessageType;
import org.refit.refitbackend.domain.user.entity.enums.UserType;

import java.time.LocalDateTime;

public interface ChatMessageCursorProjection {

    Long getMessageId();

    Long getChatId();

    Long getRoomSequence();

    Long getSenderId();

    String getSenderNickname();

    String getSenderProfileImageUrl();

    UserType getSenderUserType();

    MessageType getMessageType();

    String getContent();

    String getClientMessageId();

    LocalDateTime getCreatedAt();
}
