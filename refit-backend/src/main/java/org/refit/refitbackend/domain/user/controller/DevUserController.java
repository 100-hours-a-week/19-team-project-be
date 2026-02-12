package org.refit.refitbackend.domain.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.user.dto.DevUserReq;
import org.refit.refitbackend.domain.user.service.DevUserService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.user.UserSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"dev", "local"})
@Tag(name = "DevUser", description = "개발용 유저 전환 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev/users")
public class DevUserController {

    private final DevUserService devUserService;

    @UserSwaggerSpec.ChangeUserTypeDev
    @PatchMapping("/{user_id}/type")
    public ResponseEntity<ApiResponse<Void>> changeUserType(
            @PathVariable("user_id") @Positive(message = "expert_user_id_invalid") Long userId,
            @Valid @RequestBody DevUserReq.ChangeUserType request
    ) {
        devUserService.changeUserType(userId, request);
        return ResponseUtil.ok("success");
    }
}
