package org.refit.refitbackend.domain.jobposting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskReq;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskRes;
import org.refit.refitbackend.domain.jobposting.service.JobPostTaskService;
import org.refit.refitbackend.global.response.ApiResponse;
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
@RequestMapping("/api/v2/job-posts")
public class JobPostTaskController {

    private final JobPostTaskService jobPostTaskService;

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<JobPostTaskRes.TaskResult>> createTask(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody JobPostTaskReq.CreateTask request
    ) {
        return ResponseUtil.created("task_created", jobPostTaskService.createTask(principal.getUserId(), request));
    }

    @GetMapping("/tasks/{task_id}")
    public ResponseEntity<ApiResponse<JobPostTaskRes.TaskResult>> getTask(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("task_id") String taskId
    ) {
        return ResponseUtil.ok("success", jobPostTaskService.getTask(principal.getUserId(), taskId));
    }

    @PostMapping("/tasks/{task_id}/complete")
    public ResponseEntity<ApiResponse<JobPostTaskRes.TaskResult>> completeTask(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("task_id") String taskId,
            @Valid @RequestBody JobPostTaskReq.CompleteTask request
    ) {
        return ResponseUtil.ok("success", jobPostTaskService.completeTask(principal.getUserId(), taskId, request));
    }

    @GetMapping("/{job_post_id}")
    public ResponseEntity<ApiResponse<JobPostTaskRes.JobPostSimple>> getJobPost(
            @PathVariable("job_post_id") Long jobPostId
    ) {
        return ResponseUtil.ok("success", jobPostTaskService.getJobPost(jobPostId));
    }
}
