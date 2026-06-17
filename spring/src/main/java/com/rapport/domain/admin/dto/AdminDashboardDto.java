package com.rapport.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

public class AdminDashboardDto {

    @Getter
    @Builder
    public static class DashboardResponse {
        private UserStats userStats;
        private BookingStats bookingStats;
        private CounselorStats counselorStats;
    }

    @Getter
    @Builder
    public static class UserStats {
        private long totalUsers;
        private long totalClients;
        private long totalCounselors;
        private long newUsersToday;
        private long newUsersThisWeek;
        private long newUsersThisMonth;
    }

    @Getter
    @Builder
    public static class BookingStats {
        private long totalBookings;
        private long pendingBookings;
        private long acceptedBookings;
        private long completedBookings;
        private long cancelledBookings;
    }

    @Getter
    @Builder
    public static class CounselorStats {
        private long totalCounselors;
        private long pendingApproval;
        private long approvedCounselors;
        private long rejectedCounselors;
    }
}
