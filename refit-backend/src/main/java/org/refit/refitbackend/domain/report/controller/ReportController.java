package org.refit.refitbackend.domain.report.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.auth.jwt.CustomUserDetails;
import org.refit.refitbackend.domain.report.dto.ReportReq;
import org.refit.refitbackend.domain.report.dto.ReportRes;
import org.refit.refitbackend.domain.report.service.ReportService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.report.ReportSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ReportSwaggerSpec.CreateReportV2
    public ResponseEntity<ApiResponse<ReportRes.ReportId>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReportReq.Create request
    ) {
        return ResponseUtil.created("success", reportService.create(principal.getUserId(), request));
    }

    @GetMapping
    @ReportSwaggerSpec.ListReportsV2
    public ResponseEntity<ApiResponse<ReportRes.ReportListResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseUtil.ok("success", reportService.listMyReports(principal.getUserId()));
    }

    @GetMapping("/{report_id}")
    @ReportSwaggerSpec.GetReportDetailV2
    public ResponseEntity<ApiResponse<ReportRes.ReportDetail>> detail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("report_id") @Positive Long reportId
    ) {
        return ResponseUtil.ok("success", reportService.getDetail(principal.getUserId(), reportId));
    }

    @DeleteMapping("/{report_id}")
    @ReportSwaggerSpec.DeleteReportV2
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("report_id") @Positive Long reportId
    ) {
        reportService.delete(principal.getUserId(), reportId);
        return ResponseUtil.ok("success");
    }

    @PostMapping("/{report_id}/retry")
    @ReportSwaggerSpec.RetryReportV2
    public ResponseEntity<ApiResponse<Void>> retry(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("report_id") @Positive Long reportId
    ) {
        reportService.retry(principal.getUserId(), reportId);
        return ResponseUtil.ok("success");
    }
}
