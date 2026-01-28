package org.refit.refitbackend.domain.resume.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskReq;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskRes;
import org.refit.refitbackend.domain.resume.service.ResumeTaskService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.resume.ResumeSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resumes")
public class ResumeTaskController {

    private final ResumeTaskService resumeTaskService;

    @ResumeSwaggerSpec.ParseResumeTask
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<ResumeTaskRes.TaskResult>> parseResume(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ResumeTaskReq.Parse request
    ) {
        return ResponseUtil.ok("auto_fill_success", resumeTaskService.parseSync(principal.getUserId(), request));
    }
}
