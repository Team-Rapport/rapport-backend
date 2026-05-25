package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.service.CounselorProfileService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

    // ===== 상담사 본인 =====

    @Operation(summary = "내 프로필 조회 (상담사)")
    @GetMapping("/api/v1/counselor/profile")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<CounselorProfileDto.MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getMyProfile(principal.getId())));
    }

    @Operation(summary = "내 프로필 수정 (상담사)",
               description = """
                       수정할 필드만 보내는 부분 수정 API입니다.
                       미전송/NULL 필드는 기존 값을 유지합니다.
                       enum: counselorGender = MALE | FEMALE | ANY
                       """)
    @PatchMapping("/api/v1/counselor/profile")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<CounselorProfileDto.MyProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "프로필 수정 예시",
                                    value = """
                                            {
                                              "counselorGender": "FEMALE",
                                              "bio": "수면 문제와 불안 완화를 중심으로 일상 회복을 돕습니다.",
                                              "specializations": ["불안", "수면", "공황"],
                                              "approaches": ["인지행동치료(CBT)", "호흡/이완 훈련"],
                                              "experienceYears": 7,
                                              "officeAddress": "서울 강남구 테헤란로 101",
                                              "licenseType": "상담심리사 1급",
                                              "licenseNumber": "KCP-1-2026-1001"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CounselorProfileDto.CounselorProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("프로필이 수정되었습니다.",
                profileService.updateMyProfile(principal.getId(), request)));
    }

    // ===== 내담자용 공개 조회 =====

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
