package org.refit.refitbackend.domain.storage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.storage.dto.StorageReq;
import org.refit.refitbackend.domain.storage.service.StorageService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.swagger.spec.storage.StorageSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/uploads")
public class StorageController {

    private final StorageService storageService;

    @StorageSwaggerSpec.IssuePresignedUrl
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> issuePresignedUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody StorageReq.PresignedUrlRequest request
    ) {
        return ResponseUtil.ok("presigned_url_issued", storageService.issuePresignedUrl(principal.getUserId(), request));
    }
}
