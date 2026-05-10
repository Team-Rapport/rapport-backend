package com.rapport.domain.admin.controller;

import com.rapport.domain.admin.dto.AdminDashboardDto;
import com.rapport.domain.admin.service.AdminDashboardService;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Dashboard", description = "관리자 대시보드 API")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "관리자 대시보드 통계",
               description = "신규 가입자 수, 예약 현황, 상담사 승인 현황 통계를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardDto.DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getDashboard()));
    }
}
