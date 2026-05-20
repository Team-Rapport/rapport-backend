package com.rapport.domain.booking.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "counselor_schedule_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorScheduleSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false, unique = true)
    private User counselor;

    @Column(name = "slot_unit", nullable = false)
    private int slotUnit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public static CounselorScheduleSettings create(User counselor, int slotUnit) {
        CounselorScheduleSettings s = new CounselorScheduleSettings();
        s.counselor = counselor;
        s.slotUnit = slotUnit;
        return s;
    }

    public void updateSlotUnit(int slotUnit) {
        this.slotUnit = slotUnit;
    }
}
