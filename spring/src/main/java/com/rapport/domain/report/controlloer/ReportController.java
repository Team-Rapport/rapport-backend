package com.rapport.domain.report.controller;

import com.rapport.domain.report.dto.ReportDto;
import com.rapport.domain.report.service.ReportService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report", description = "AI 사전 점검 리포트 API")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "리포트 저장", description = "FastAPI에서 분석 완료 후 호출. 세션도 자동 완료 처리됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportDto.ReportDetail>> saveReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportDto.SaveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("리포트가 저장되었습니다.",
                reportService.saveReport(principal.getId(), request)));
    }

    @Operation(summary = "내 리포트 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReportDto.ReportSummary>>> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getMyReports(principal.getId(), pageable)));
    }

    @Operation(summary = "리포트 상세 조회")
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDto.ReportDetail>> getReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getReport(reportId, principal.getId())));
    }

    @Operation(summary = "세션 ID로 리포트 조회")
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<ReportDto.ReportDetail>> getReportBySession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getReportBySession(sessionId, principal.getId())));
    }
}
