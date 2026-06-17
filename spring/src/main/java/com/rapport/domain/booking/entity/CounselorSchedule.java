package com.rapport.domain.booking.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "counselor_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public static CounselorSchedule create(User counselor, SessionType sessionType,
                                            LocalDate slotDate, LocalTime startTime,
                                            LocalTime endTime) {
        CounselorSchedule s = new CounselorSchedule();
        s.counselor = counselor;
        s.sessionType = sessionType;
        s.slotDate = slotDate;
        s.startTime = startTime;
        s.endTime = endTime;
        return s;
    }

    public void markUnavailable() { this.isAvailable = false; }
    public void markAvailable()   { this.isAvailable = true; }
}
