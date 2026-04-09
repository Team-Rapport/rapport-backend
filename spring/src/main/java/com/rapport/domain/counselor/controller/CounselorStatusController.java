package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rapport.domain.counselor.service.CounselorApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Counselor", description = "상담사 전용 API")
@RestController
@RequestMapping("/api/v1/counselor")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CounselorStatusController {

    private final CounselorProfileRepository counselorProfileRepository;
    private final CounselorApprovalService counselorApprovalService;

    /**
     * 내 심사 상태 조회 — 프론트에서 PENDING/REJECTED/APPROVED 화면 분기에 사용
     */
    @Operation(summary = "심사 상태 조회", description = "상담사 본인의 현재 심사 상태를 조회합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ApprovalStatusResponse>> getMyStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        CounselorProfile profile = counselorProfileRepository
                .findByUserId(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));

        ApprovalStatusResponse response = ApprovalStatusResponse.builder()
                .approvalStatus(profile.getApprovalStatus())
                .rejectionReason(profile.getRejectionReason())
                .approvedAt(profile.getApprovedAt() != null
                        ? profile.getApprovedAt().toString() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    @Operation(summary = "상담사 심사 재신청", description = "REJECTED → PENDING으로 변경")
    @PostMapping("/reapply")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<Void>> reapply(
            @AuthenticationPrincipal UserPrincipal principal) {
        counselorApprovalService.reapply(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("재신청이 완료되었습니다. 관리자 심사를 기다려주세요."));
    }

    @Getter
    @Builder
    static class ApprovalStatusResponse {
        private CounselorProfile.ApprovalStatus approvalStatus;
        private String rejectionReason;
        private String approvedAt;
    }

}
