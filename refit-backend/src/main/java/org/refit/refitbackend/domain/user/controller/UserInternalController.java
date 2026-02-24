package org.refit.refitbackend.domain.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.domain.user.service.UserService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.user.UserInternalSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
@Validated
@Tag(name = "Internal-User", description = "내부 연동용 유저 API")
public class UserInternalController {

    private final UserService userService;

    @UserInternalSwaggerSpec.GetUserInternal
    @GetMapping("/{user_id}")
    public ResponseEntity<ApiResponse<UserRes.Detail>> getUser(
            @Parameter(description = "유저 ID", example = "1", required = true)
            @PathVariable("user_id") @Positive(message = "user_id_invalid") Long userId
    ) {
        return ResponseUtil.ok("success", userService.getUser(userId));
    }
}
