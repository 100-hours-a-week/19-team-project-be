package org.refit.refitbackend.domain.resume.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.resume.dto.ResumeReq;
import org.refit.refitbackend.domain.resume.dto.ResumeRes;
import org.refit.refitbackend.domain.resume.service.ResumeService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.resume.ResumeSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @ResumeSwaggerSpec.CreateResume
    @PostMapping
    public ResponseEntity<ApiResponse<ResumeRes.ResumeId>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ResumeReq.Create request
    ) {
        return ResponseUtil.created("success", resumeService.create(principal.getUserId(), request));
    }

    @ResumeSwaggerSpec.ListResumes
    @GetMapping
    public ResponseEntity<ApiResponse<ResumeRes.ResumeListResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseUtil.ok("success", resumeService.getMyResumes(principal.getUserId()));
    }

    @ResumeSwaggerSpec.GetResumeDetail
    @GetMapping("/{resume_id}")
    public ResponseEntity<ApiResponse<ResumeRes.ResumeDetail>> detail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("resume_id") @Positive(message = "유효하지 않은 이력서 ID입니다.") Long resumeId
    ) {
        return ResponseUtil.ok("success", resumeService.getDetail(principal.getUserId(), resumeId));
    }

    @ResumeSwaggerSpec.UpdateResume
    @PatchMapping("/{resume_id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("resume_id") @Positive(message = "유효하지 않은 이력서 ID입니다.") Long resumeId,
            @Valid @RequestBody ResumeReq.Update request
    ) {
        resumeService.update(principal.getUserId(), resumeId, request);
        return ResponseUtil.ok("success");
    }

    @ResumeSwaggerSpec.UpdateResumeTitle
    @PatchMapping("/{resume_id}/title")
    public ResponseEntity<ApiResponse<Void>> updateTitle(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("resume_id") @Positive(message = "유효하지 않은 이력서 ID입니다.") Long resumeId,
            @Valid @RequestBody ResumeReq.UpdateTitle request
    ) {
        resumeService.updateTitle(principal.getUserId(), resumeId, request);
        return ResponseUtil.ok("success");
    }

    @ResumeSwaggerSpec.DeleteResume
    @DeleteMapping("/{resume_id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("resume_id") @Positive(message = "유효하지 않은 이력서 ID입니다.") Long resumeId
    ) {
        resumeService.delete(principal.getUserId(), resumeId);
        return ResponseUtil.ok("success");
    }
}
