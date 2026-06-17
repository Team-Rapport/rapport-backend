package com.rapport.domain.user.controller;

import com.rapport.domain.auth.dto.AuthDto;
import com.rapport.domain.auth.service.AuthService;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.report.entity.ReportRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.domain.user.service.UserService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import com.rapport.global.util.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

import java.time.LocalDate;

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
    private final AuthService authService;
    private final UserService userService;

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

    @Operation(summary = "내 기본 프로필 수정",
               description = "OAuth 신규 회원 추가 입력용. 이름/전화번호는 필수이며, 성별/생년월일은 선택입니다.")
    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMyProfileRequest request) {
        userService.updateMyProfile(
                principal.getId(),
                request.getName(),
                request.getPhone(),
                request.getGender(),
                request.getBirthDate()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "프로필이 수정되었습니다.",
                authService.getMe(principal.getId())
        ));
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

    @Operation(summary = "비밀번호 변경",
               description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다. 다시 로그인해주세요."));
    }

    @Operation(summary = "회원 탈퇴",
               description = "소셜/이메일 계정 공통 탈퇴. 이메일 계정은 비밀번호 확인이 필요합니다. 개인정보는 즉시 익명 처리됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) AuthDto.WithdrawRequest request) {
        String password = (request != null) ? request.getPassword() : null;
        authService.withdraw(principal.getId(), password);
        return ResponseEntity.ok(ApiResponse.ok("회원 탈퇴가 완료되었습니다."));
    }

    // ===== DTO =====

    @Getter
    static class UpdateNameRequest {
        @Size(max = 100, message = "이름은 100자 이내로 입력해주세요.")
        private String name;
    }

    @Getter
    static class UpdateMyProfileRequest {
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 100, message = "이름은 100자 이내로 입력해주세요.")
        private String name;

        @NotBlank(message = "전화번호를 입력해주세요.")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
                 message = "전화번호 형식은 010-1234-5678 이어야 합니다.")
        private String phone;

        private User.Gender gender;
        private LocalDate birthDate;

        public void setName(String name) {
            this.name = name == null ? null : name.trim();
        }

        public void setPhone(String phone) {
            this.phone = phone == null ? null : phone.trim();
        }
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
