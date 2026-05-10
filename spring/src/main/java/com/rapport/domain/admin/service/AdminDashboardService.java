package com.rapport.domain.admin.service;

import com.rapport.domain.admin.dto.AdminDashboardDto;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CounselorProfileRepository counselorProfileRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto.DashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = startOfDay.minusDays(startOfDay.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        AdminDashboardDto.UserStats userStats = AdminDashboardDto.UserStats.builder()
                .totalUsers(userRepository.count())
                .totalClients(userRepository.countByRole(User.Role.CLIENT))
                .totalCounselors(userRepository.countByRole(User.Role.COUNSELOR))
                .newUsersToday(userRepository.countByCreatedAtBetween(startOfDay, now))
                .newUsersThisWeek(userRepository.countByCreatedAtBetween(startOfWeek, now))
                .newUsersThisMonth(userRepository.countByCreatedAtBetween(startOfMonth, now))
                .build();

        AdminDashboardDto.BookingStats bookingStats = AdminDashboardDto.BookingStats.builder()
                .totalBookings(bookingRepository.count())
                .pendingBookings(bookingRepository.countByStatus(Booking.BookingStatus.PENDING))
                .acceptedBookings(bookingRepository.countByStatus(Booking.BookingStatus.ACCEPTED))
                .completedBookings(bookingRepository.countByStatus(Booking.BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository.countByStatus(Booking.BookingStatus.CANCELLED))
                .build();

        AdminDashboardDto.CounselorStats counselorStats = AdminDashboardDto.CounselorStats.builder()
                .totalCounselors(userRepository.countByRole(User.Role.COUNSELOR))
                .pendingApproval(counselorProfileRepository.countByApprovalStatus(CounselorProfile.ApprovalStatus.PENDING))
                .approvedCounselors(counselorProfileRepository.countByApprovalStatus(CounselorProfile.ApprovalStatus.APPROVED))
                .rejectedCounselors(counselorProfileRepository.countByApprovalStatus(CounselorProfile.ApprovalStatus.REJECTED))
                .build();

        return AdminDashboardDto.DashboardResponse.builder()
                .userStats(userStats)
                .bookingStats(bookingStats)
                .counselorStats(counselorStats)
                .build();
    }
}
