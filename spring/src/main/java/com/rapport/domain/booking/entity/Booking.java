package com.rapport.domain.booking.entity;

import com.rapport.domain.report.entity.Report;
import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private CounselorSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report attachedReport;

    // ★ V2 마이그레이션으로 추가된 필드
    @Column(columnDefinition = "TEXT")
    private String concern; // 예약 시 내담자가 입력한 주요 고민

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "booked_date", nullable = false)
    private LocalDate bookedDate;

    @Column(name = "booked_start_time", nullable = false)
    private LocalTime bookedStartTime;

    @Column(name = "booked_end_time", nullable = false)
    private LocalTime bookedEndTime;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 20)
    private CancelledBy cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ===== 팩토리 =====
    public static Booking create(User client, User counselor,
                                  CounselorSchedule schedule, SessionType sessionType,
                                  Report attachedReport, String concern) {
        Booking b = new Booking();
        b.client = client;
        b.counselor = counselor;
        b.schedule = schedule;
        b.sessionType = sessionType;
        b.attachedReport = attachedReport;
        b.concern = concern;
        b.bookedDate = schedule.getSlotDate();
        b.bookedStartTime = schedule.getStartTime();
        b.bookedEndTime = schedule.getEndTime();
        return b;
    }

    // ===== 도메인 메서드 =====
    public void accept() {
        validatePending();
        this.status = BookingStatus.ACCEPTED;
    }

    public void reject() {
        validatePending();
        this.status = BookingStatus.REJECTED;
        this.schedule.markAvailable();
    }

    public void cancelByClient(String reason) {
        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("취소할 수 없는 상태입니다.");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledBy = CancelledBy.CLIENT;
        this.cancelledAt = LocalDateTime.now();
        this.schedule.markAvailable();
    }

    public void complete() { this.status = BookingStatus.COMPLETED; }

    private void validatePending() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약이 아닙니다.");
        }
    }

    public boolean isPending()  { return status == BookingStatus.PENDING; }
    public boolean isAccepted() { return status == BookingStatus.ACCEPTED; }

    public enum BookingStatus { PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED }
    public enum CancelledBy   { CLIENT, COUNSELOR, SYSTEM }
}
