package com.rapport.domain.dashboard;

import com.rapport.domain.booking.dto.BookingDto;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.booking.service.BookingService;
import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.notification.entity.NotificationRepository;
import com.rapport.domain.report.dto.ReportDto;
import com.rapport.domain.report.entity.Report;
import com.rapport.domain.report.entity.ReportRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// ══════════════════════════════════════════════════════════════
// Service
// ══════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
class DashboardService {

    private final BookingRepository          bookingRepository;
    private final BookingService             bookingService;
    private final ReportRepository           reportRepository;
    private final NotificationRepository     notificationRepository;
    private final CounselorProfileRepository counselorProfileRepository;

    @Transactional(readOnly = true)
    public ClientDashboard getClientDashboard(Long clientId) {

        // 예약 확정된 것 중 오늘 이후 최대 3건
        List<BookingDto.BookingResponse> upcoming =
                bookingRepository.findUpcomingByClient(clientId, LocalDate.now())
                        .stream().limit(3)
                        .map(bookingService::toResponse)
                        .toList();

        // 최근 리포트 2건 (홈 화면 카드 2개)
        List<ReportDto.ReportSummary> recentReports =
                reportRepository.findAllByClientIdOrderByCreatedAtDesc(
                                clientId, PageRequest.of(0, 2))
                        .stream().map(this::toReportSummary).toList();

        // 추천 상담사 5건 (평점 높은 순)
        List<CounselorProfileDto.PublicProfileResponse> recommendedCounselors =
                counselorProfileRepository.findAllByApprovalStatus(
                                CounselorProfile.ApprovalStatus.APPROVED,
                                PageRequest.of(0, 5))
                        .stream().map(this::toPublicProfile).toList();

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(clientId);

        return ClientDashboard.builder()
                .upcomingBookings(upcoming)
                .recentReports(recentReports)
                .recommendedCounselors(recommendedCounselors)
                .unreadNotificationCount(unreadCount)
                .build();
    }

    @Transactional(readOnly = true)
    public CounselorDashboard getCounselorDashboard(Long counselorId) {

        // 오늘 확정 예약 전체
        List<BookingDto.BookingResponse> todaySchedule =
                bookingRepository.findTodayByCounselor(counselorId, LocalDate.now())
                        .stream().map(bookingService::toResponse).toList();

        // 수락 대기 중인 예약 수
        long pendingCount = bookingRepository
                .countByCounselorIdAndStatus(counselorId, Booking.BookingStatus.PENDING);

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(counselorId);

        return CounselorDashboard.builder()
                .todaySchedule(todaySchedule)
                .pendingBookingCount(pendingCount)
                .unreadNotificationCount(unreadCount)
                .build();
    }

    private ReportDto.ReportSummary toReportSummary(Report r) {
        return ReportDto.ReportSummary.builder()
                .reportId(r.getId())
                .sessionId(r.getSession().getId())
                .depressionScore(r.getDepressionScore())
                .anxietyScore(r.getAnxietyScore())
                .stressScore(r.getStressScore())
                .riskLevel(r.getRiskLevel())
                .crisisDetected(r.isCrisisDetected())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private CounselorProfileDto.PublicProfileResponse toPublicProfile(CounselorProfile p) {
        return CounselorProfileDto.PublicProfileResponse.builder()
                .userId(p.getUser().getId())
                .name(p.getUser().getName())
                .profileImageUrl(p.getUser().getProfileImageUrl())
                .licenseType(p.getLicenseType())
                .counselorGender(p.getCounselorGender())
                .specializations(p.getSpecializations())
                .approaches(p.getApproaches())
                .bio(p.getBio())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .approvalStatus(p.getApprovalStatus())
                .approvedAt(p.getApprovedAt())
                .build();
    }

    @Getter @Builder
    public static class ClientDashboard {
        private List<BookingDto.BookingResponse>                upcomingBookings;
        private List<ReportDto.ReportSummary>                   recentReports;
        private List<CounselorProfileDto.PublicProfileResponse> recommendedCounselors;
        private long                                             unreadNotificationCount;
    }

    @Getter @Builder
    public static class CounselorDashboard {
        private List<BookingDto.BookingResponse> todaySchedule;
        private long                             pendingBookingCount;
        private long                             unreadNotificationCount;
    }
}

// ══════════════════════════════════════════════════════════════
// Controller
// ══════════════════════════════════════════════════════════════
@Tag(name = "Dashboard", description = "홈 대시보드 데이터 API")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "내담자 홈 대시보드",
               description = "예약 현황(3건) + 최근 리포트(2건) + 추천 상담사(5건) + 읽지 않은 알림 수")
    @GetMapping("/client")
    public ResponseEntity<ApiResponse<DashboardService.ClientDashboard>> getClientDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getClientDashboard(principal.getId())));
    }

    @Operation(summary = "상담사 홈 대시보드",
               description = "오늘 일정 + 미처리 예약 수 + 읽지 않은 알림 수")
    @GetMapping("/counselor")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<DashboardService.CounselorDashboard>> getCounselorDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getCounselorDashboard(principal.getId())));
    }
}
