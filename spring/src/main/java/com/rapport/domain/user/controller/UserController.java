package com.rapport.domain.user.controller;

import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.report.entity.ReportRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import com.rapport.global.util.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "회원정보 수정 및 마이페이지 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReportRepository reportRepository;
    private final S3Service s3Service;

    @Operation(summary = "마이페이지 통계 조회",
               description = "상담 횟수, 리포트 수")
    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<MyStats>> getMyStats(
            @AuthenticationPrincipal UserPrincipal principal) {

        long counselingCount = bookingRepository.countByClientIdAndStatus(
                principal.getId(), Booking.BookingStatus.COMPLETED);

        long reportCount = reportRepository.findAllByClientIdOrderByCreatedAtDesc(
                        principal.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(ApiResponse.ok(MyStats.builder()
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .counselingCount(counselingCount)
                .reportCount(reportCount)
                .build()));
    }

    @Operation(summary = "닉네임 수정")
    @PatchMapping("/me/name")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateName(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateNameRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateName(request.getName());
        return ResponseEntity.ok(ApiResponse.ok("이름이 수정되었습니다."));
    }

    @Operation(summary = "프로필 사진 업로드")
    @PatchMapping(value = "/me/profile-image",
                  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<ApiResponse<String>> updateProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getProfileImageUrl() != null) {
            s3Service.delete(user.getProfileImageUrl());
        }
        String url = s3Service.upload(file, "profiles/" + principal.getId());
        user.updateProfileImage(url);
        return ResponseEntity.ok(ApiResponse.ok("프로필 사진이 변경되었습니다.", url));
    }

    // ── DTO ─────────────────────────────────────────────────

    @Getter
    static class UpdateNameRequest {
        @Size(max = 100, message = "이름은 100자 이내로 입력해주세요.")
        private String name;
    }

    @Getter
    @Builder
    static class MyStats {
        private String name;
        private String email;
        private String profileImageUrl;
        private long counselingCount;
        private long reportCount;
    }
}
