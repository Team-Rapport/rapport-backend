package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.service.CounselorProfileService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Counselor Profile", description = "상담사 프로필 CRUD API")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CounselorProfileController {

    private final CounselorProfileService profileService;

    // ── 상담사 본인 ──────────────────────────────────────────

    @Operation(summary = "내 프로필 조회 (상담사)")
    @GetMapping("/api/v1/counselor/profile")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<CounselorProfileDto.MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getMyProfile(principal.getId())));
    }

    @Operation(summary = "내 프로필 수정 (상담사)",
               description = "수정할 필드만 보내도 됩니다. null 필드는 변경되지 않습니다.")
    @PatchMapping("/api/v1/counselor/profile")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<CounselorProfileDto.MyProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CounselorProfileDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("프로필이 수정되었습니다.",
                profileService.updateMyProfile(principal.getId(), request)));
    }

    // ── 내담자용 공개 조회 ────────────────────────────────────

    @Operation(summary = "승인된 상담사 목록 조회 (내담자)")
    @GetMapping("/api/v1/counselors")
    public ResponseEntity<ApiResponse<Page<CounselorProfileDto.PublicProfileResponse>>> getCounselors(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getApprovedCounselors(pageable)));
    }

    @Operation(summary = "상담사 프로필 상세 조회 (내담자)")
    @GetMapping("/api/v1/counselors/{userId}")
    public ResponseEntity<ApiResponse<CounselorProfileDto.PublicProfileResponse>> getCounselorProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getPublicProfile(userId)));
    }
}
