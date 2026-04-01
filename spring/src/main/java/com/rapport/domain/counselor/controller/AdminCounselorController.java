package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.dto.CounselorDto;
import com.rapport.domain.counselor.service.CounselorApprovalService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Counselor", description = "관리자 전용 상담사 심사 API")
@RestController
@RequestMapping("/api/v1/admin/counselors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminCounselorController {

    private final CounselorApprovalService counselorApprovalService;

    @Operation(summary = "심사 대기 목록 조회", description = "PENDING 상태의 상담사 목록을 페이지로 조회합니다.")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<CounselorDto.PendingCounselorResponse>>> getPendingCounselors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok(counselorApprovalService.getPendingCounselors(pageable)));
    }

    @Operation(summary = "상담사 승인", description = "PENDING → APPROVED 처리 및 승인 이메일 발송")
    @PatchMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<CounselorDto.ApprovalResultResponse>> approveCounselor(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok("승인 처리가 완료되었습니다.", counselorApprovalService.approve(userId)));
    }

    @Operation(summary = "상담사 반려", description = "PENDING → REJECTED 처리 및 반려 사유 이메일 발송")
    @PatchMapping("/{userId}/reject")
    public ResponseEntity<ApiResponse<CounselorDto.ApprovalResultResponse>> rejectCounselor(
            @PathVariable Long userId,
            @Valid @RequestBody CounselorDto.RejectRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("반려 처리가 완료되었습니다.",
                        counselorApprovalService.reject(userId, request.getReason())));
    }
}
