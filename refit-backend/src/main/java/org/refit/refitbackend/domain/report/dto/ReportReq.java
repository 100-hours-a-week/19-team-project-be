package org.refit.refitbackend.domain.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ReportReq {

    public record Create(
            @NotNull(message = "chat_id_required")
            @Positive(message = "chat_id_required")
            Long chatRoomId
    ) {}
}
