package org.refit.refitbackend.domain.master.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.master.dto.MasterRes;
import org.refit.refitbackend.domain.master.service.MasterService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.swagger.spec.master.MasterSwaggerSpec;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Master", description = "마스터 데이터 조회 API (직무, 스킬, 경력 레벨)")
public class MasterController {

    private final MasterService masterService;

    /* =======================
     * 직무 목록 조회
     * ======================= */
    @MasterSwaggerSpec.Jobs
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<MasterRes.Jobs>> getJobs() {
        MasterRes.Jobs result = masterService.getJobs();
        int count = result.jobs() == null ? 0 : result.jobs().size();
        log.info("GET /api/v1/jobs -> count={}", count);
        return ResponseUtil.ok("success", result);
    }

    /* =======================
     * 경력 레벨 목록 조회
     * ======================= */
    @MasterSwaggerSpec.CareerLevels
    @GetMapping("/career-levels")
    public ResponseEntity<ApiResponse<MasterRes.CareerLevels>> getCareerLevels() {
        MasterRes.CareerLevels result = masterService.getCareerLevels();
        int count = result.careerLevels() == null ? 0 : result.careerLevels().size();
        log.info("GET /api/v1/career-levels -> count={}", count);
        return ResponseUtil.ok("success", result);
    }

    /* =======================
     * 스킬 목록 조회 (검색 가능)
     * ======================= */
    @MasterSwaggerSpec.Skills
    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<MasterRes.Skills>> getSkills(
            @Parameter(
                    description = "스킬 이름 검색어 (부분 일치)",
                    example = "java"
            )
            @RequestParam(required = false) String keyword
    ) {
        MasterRes.Skills result = masterService.getSkills(keyword);
        int count = result.skills() == null ? 0 : result.skills().size();
        log.info("GET /api/v1/skills -> keyword={}, count={}", keyword, count);
        return ResponseUtil.ok("success", result);
    }

    /* =======================
     * 이메일 도메인 목록 조회 (커서 페이지네이션)
     * ======================= */
    @MasterSwaggerSpec.EmailDomains
    @GetMapping("/email-domains")
    public ResponseEntity<ApiResponse<MasterRes.EmailDomains>> getEmailDomains(
            @Parameter(description = "다음 페이지 커서 (마지막 도메인 값)", example = "navercorp.com")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 사이즈", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        MasterRes.EmailDomains result = masterService.getEmailDomains(cursor, size);
        int count = result.emailDomains() == null ? 0 : result.emailDomains().size();
        log.info("GET /api/v1/email-domains -> cursor={}, size={}, count={}", cursor, size, count);
        return ResponseUtil.ok("success", result);
    }
}
