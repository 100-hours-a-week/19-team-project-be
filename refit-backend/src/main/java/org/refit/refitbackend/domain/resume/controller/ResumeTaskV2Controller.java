package org.refit.refitbackend.domain.resume.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskReq;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskRes;
import org.refit.refitbackend.domain.resume.service.ResumeTaskService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.resume.ResumeTaskV2SwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/resumes")
public class ResumeTaskV2Controller {

    private final ResumeTaskService resumeTaskService;

    @PostMapping("/tasks")
    @ResumeTaskV2SwaggerSpec.CreateParseTaskV2
    public ResponseEntity<ApiResponse<ResumeTaskRes.TaskResult>> createParseTask(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ResumeTaskReq.CreateTask request
    ) {
        return ResponseUtil.created("task_created", resumeTaskService.createAsyncTask(principal.getUserId(), request));
    }

    @GetMapping("/tasks/{task_id}")
    @ResumeTaskV2SwaggerSpec.GetParseTaskV2
    public ResponseEntity<ApiResponse<ResumeTaskRes.TaskResult>> getParseTask(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("task_id") String taskId
    ) {
        return ResponseUtil.ok("success", resumeTaskService.getTask(principal.getUserId(), taskId));
    }
}
