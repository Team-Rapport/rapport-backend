package com.rapport.domain.intakeform.entity;

import com.rapport.domain.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "intake_forms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntakeForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "form_data", nullable = false, columnDefinition = "JSON")
    private String formData;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static IntakeForm create(Booking booking, String formDataJson) {
        IntakeForm form = new IntakeForm();
        form.booking = booking;
        form.formData = formDataJson;
        form.submittedAt = LocalDateTime.now();
        return form;
    }

    public void updateFormData(String formDataJson) {
        this.formData = formDataJson;
        this.submittedAt = LocalDateTime.now();
    }
}
