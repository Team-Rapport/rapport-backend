package com.rapport.domain.booking.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "counselor_dayoffs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorDayoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @Enumerated(EnumType.STRING)
    @Column(name = "dayoff_type", nullable = false, length = 30)
    private DayoffType dayoffType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "dayoff_date")
    private LocalDate dayoffDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum DayoffType { BREAKTIME, REGULAR_HOLIDAY, TEMPORARY_HOLIDAY }

    public static CounselorDayoff createRegularBreaktime(User counselor, LocalTime startTime, LocalTime endTime) {
        CounselorDayoff d = new CounselorDayoff();
        d.counselor = counselor;
        d.dayoffType = DayoffType.BREAKTIME;
        d.startTime = startTime;
        d.endTime = endTime;
        return d;
    }

    public static CounselorDayoff createTemporaryBreaktime(User counselor, LocalDate date,
                                                            LocalTime startTime, LocalTime endTime) {
        CounselorDayoff d = new CounselorDayoff();
        d.counselor = counselor;
        d.dayoffType = DayoffType.BREAKTIME;
        d.dayoffDate = date;
        d.startTime = startTime;
        d.endTime = endTime;
        return d;
    }

    public static CounselorDayoff createRegularHoliday(User counselor, DayOfWeek dayOfWeek) {
        CounselorDayoff d = new CounselorDayoff();
        d.counselor = counselor;
        d.dayoffType = DayoffType.REGULAR_HOLIDAY;
        d.dayOfWeek = dayOfWeek;
        return d;
    }

    public static CounselorDayoff createTemporaryHoliday(User counselor, LocalDate date) {
        CounselorDayoff d = new CounselorDayoff();
        d.counselor = counselor;
        d.dayoffType = DayoffType.TEMPORARY_HOLIDAY;
        d.dayoffDate = date;
        return d;
    }
}
