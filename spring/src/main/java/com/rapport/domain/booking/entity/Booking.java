package com.rapport.domain.booking.entity;

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

    @Column(name = "case_id")
    private Long caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "session_type_id", nullable = false)
    private Long sessionTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "booked_date")
    private LocalDate bookedDate;

    @Column(name = "booked_start_time")
    private LocalTime bookedStartTime;

    @Column(name = "booked_end_time")
    private LocalTime bookedEndTime;

    @Column(name = "concern")
    private String concern;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Booking create(User client, User counselor, Long sessionTypeId,
                                 LocalDate bookedDate, LocalTime startTime, LocalTime endTime,
                                 String concern) {
        Booking b = new Booking();
        b.client = client;
        b.counselor = counselor;
        b.sessionTypeId = sessionTypeId != null ? sessionTypeId : 1L;
        b.bookedDate = bookedDate != null ? bookedDate : LocalDate.now();
        b.bookedStartTime = startTime != null ? startTime : LocalTime.of(10, 0);
        b.bookedEndTime = endTime != null ? endTime : LocalTime.of(11, 0);
        b.concern = concern;
        return b;
    }

    public void confirm() {
        this.status = BookingStatus.ACCEPTED;
    }

    public void reject() {
        this.status = BookingStatus.REJECTED;
    }

    public enum BookingStatus { PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED }

    public enum CancelledBy { CLIENT, COUNSELOR, SYSTEM }
}
