package org.refit.refitbackend.domain.chat.repository.projection;

import org.refit.refitbackend.domain.chat.entity.ChatRequestType;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.user.entity.enums.UserType;

import java.time.LocalDateTime;

public interface ChatRoomListProjection {

    Long getChatId();

    Long getRequesterId();

    String getRequesterNickname();

    String getRequesterProfileImageUrl();

    UserType getRequesterUserType();

    Long getReceiverId();

    String getReceiverNickname();

    String getReceiverProfileImageUrl();

    UserType getReceiverUserType();

    Long getLastMessageId();

    Long getLastMessageRoomSequence();

    String getLastMessageContent();

    LocalDateTime getLastMessageAt();

    Long getUnreadCount();

    ChatRoomStatus getStatus();

    ChatRequestType getRequestType();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
