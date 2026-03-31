package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.dto.CounselorDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Counselor", description = "상담사 API")
@RestController
@RequestMapping("/api/v1/counselors")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CounselorController {

    private final CounselorProfileRepository counselorProfileRepository;

    /**
     * 내 심사 상태 조회 (PENDING / APPROVED / REJECTED)
     */
    @Operation(summary = "내 심사 상태 조회")
    @GetMapping("/me/status")
    public ResponseEntity<ApiResponse<ApprovalStatusResponse>> getMyApprovalStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        CounselorProfile profile = counselorProfileRepository
                .findByUserId(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));

        ApprovalStatusResponse response = new ApprovalStatusResponse(
                profile.getApprovalStatus(),
                profile.getRejectionReason(),
                profile.getApprovedAt() != null ? profile.getApprovedAt().toString() : null
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record ApprovalStatusResponse(
            CounselorProfile.ApprovalStatus status,
            String rejectionReason,
            String approvedAt
    ) {}
}
