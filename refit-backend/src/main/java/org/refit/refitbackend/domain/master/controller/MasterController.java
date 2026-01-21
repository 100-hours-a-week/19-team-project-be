package org.refit.refitbackend.domain.master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.master.dto.MasterRes;
import org.refit.refitbackend.domain.master.service.MasterService;
import org.refit.refitbackend.global.response.ApiResponse;
import org.refit.refitbackend.global.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "Master", description = "마스터 데이터 조회 API (직무, 스킬, 경력 레벨)")
public class MasterController {

    private final MasterService masterService;

    /* =======================
     * 직무 목록 조회
     * ======================= */
    @Operation(
            summary = "직무 목록 조회",
            description = "회원가입 및 필터링에 사용되는 전체 직무 목록을 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "직무 목록 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = MasterRes.Jobs.class)
                            )
                    )
            }
    )
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<MasterRes.Jobs>> getJobs() {
        return ResponseUtil.ok("success", masterService.getJobs());
    }

    /* =======================
     * 경력 레벨 목록 조회
     * ======================= */
    @Operation(
            summary = "경력 레벨 목록 조회",
            description = "선택 가능한 전체 경력 레벨 목록을 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "경력 레벨 목록 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = MasterRes.CareerLevels.class)
                            )
                    )
            }
    )
    @GetMapping("/career-levels")
    public ResponseEntity<ApiResponse<MasterRes.CareerLevels>> getCareerLevels() {
        return ResponseUtil.ok("success", masterService.getCareerLevels());
    }

    /* =======================
     * 스킬 목록 조회 (검색 가능)
     * ======================= */
    @Operation(
            summary = "스킬 목록 조회",
            description = """
                    선택 가능한 전체 기술 스택 목록을 조회합니다.
                    
                    - keyword 파라미터가 없으면 전체 조회
                    - keyword가 있으면 이름 기준 부분 검색
                    """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "스킬 목록 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = MasterRes.Skills.class)
                            )
                    )
            }
    )
    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<MasterRes.Skills>> getSkills(
            @Parameter(
                    description = "스킬 이름 검색어 (부분 일치)",
                    example = "java"
            )
            @RequestParam(required = false) String keyword
    ) {
        return ResponseUtil.ok("success", masterService.getSkills(keyword));
    }
}
