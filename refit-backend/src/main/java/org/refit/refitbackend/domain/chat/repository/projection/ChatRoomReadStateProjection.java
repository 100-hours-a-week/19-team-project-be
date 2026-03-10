package org.refit.refitbackend.domain.chat.repository.projection;

public interface ChatRoomReadStateProjection {

    Long getRoomId();

    Long getRequesterId();

    Long getReceiverId();

    Long getLastMessageSeq();
}
