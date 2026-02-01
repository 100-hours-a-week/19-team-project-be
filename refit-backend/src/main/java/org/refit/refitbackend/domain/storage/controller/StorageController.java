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
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @StorageSwaggerSpec.IssuePresignedDownloadUrlPost
    @PostMapping("/presigned-url/download")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> issuePresignedDownloadUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody StorageReq.PresignedDownloadRequest request
    ) {
        return ResponseUtil.ok("presigned_url_issued", storageService.issuePresignedDownloadUrl(principal.getUserId(), request));
    }

    @StorageSwaggerSpec.IssuePresignedDownloadUrlGet
    @GetMapping("/presigned-url/download")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> issuePresignedDownloadUrlGet(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(
                    name = "file_url",
                    description = "S3 원본 파일 URL",
                    required = true,
                    example = "https://refit-storage-prod.s3.ap-northeast-2.amazonaws.com/resumes/original/1/abc123.pdf"
            )
            @RequestParam("file_url") String fileUrl
    ) {
        StorageReq.PresignedDownloadRequest request = new StorageReq.PresignedDownloadRequest(fileUrl);
        return ResponseUtil.ok("presigned_url_issued", storageService.issuePresignedDownloadUrl(principal.getUserId(), request));
    }
}
