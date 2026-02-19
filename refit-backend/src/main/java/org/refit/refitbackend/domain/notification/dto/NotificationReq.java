package org.refit.refitbackend.domain.notification.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NotificationReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateReadStatus(
            Boolean isRead
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpsertFcmToken(
            @NotBlank(message = "token은 필수입니다.")
            @Size(max = 255, message = "token 길이는 255자를 초과할 수 없습니다.")
            String token
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DeleteFcmToken(
            @NotBlank(message = "token은 필수입니다.")
            @Size(max = 255, message = "token 길이는 255자를 초과할 수 없습니다.")
            String token
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestPush(
            @NotBlank(message = "token은 필수입니다.")
            @Size(max = 255, message = "token 길이는 255자를 초과할 수 없습니다.")
            String token,
            @NotBlank(message = "title은 필수입니다.")
            @Size(max = 120, message = "title 길이는 120자를 초과할 수 없습니다.")
            String title,
            @NotBlank(message = "content는 필수입니다.")
            @Size(max = 500, message = "content 길이는 500자를 초과할 수 없습니다.")
            String content
    ) {}
}
