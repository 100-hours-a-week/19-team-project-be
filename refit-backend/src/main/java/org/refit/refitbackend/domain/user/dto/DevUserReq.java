package org.refit.refitbackend.domain.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DevUserReq {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChangeUserType(
            @NotBlank(message = "user_type_invalid")
            String userType,
            String companyName,
            String companyEmail
    ) {}
}
