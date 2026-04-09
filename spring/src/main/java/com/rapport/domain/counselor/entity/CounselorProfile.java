package com.rapport.domain.counselor.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "counselor_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_type", nullable = false, length = 100)
    private String licenseType;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "counselor_gender", nullable = false, length = 10)
    private CounselorGender counselorGender = CounselorGender.ANY;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> specializations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> approaches;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years", columnDefinition = "TINYINT UNSIGNED")
    private Integer experienceYears;

    @Column(name = "office_address", length = 500)
    private String officeAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "is_verified", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isVerified = false;

    @Column(name = "average_rating", columnDefinition = "DECIMAL(3,2)")
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int reviewCount = 0;

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

    public static CounselorProfile create(User user, String licenseType, String licenseNumber,
                                          CounselorGender gender) {
        CounselorProfile profile = new CounselorProfile();
        profile.user = user;
        profile.licenseType = licenseType;
        profile.licenseNumber = licenseNumber;
        profile.counselorGender = gender;
        profile.averageRating = BigDecimal.ZERO;
        profile.specializations = List.of();
        return profile;
    }

    // ===== 도메인 메서드 =====

    public void approve(Long adminId) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 심사입니다.");
        }
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.isVerified = true;
    }

    public void reject(String reason) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 심사입니다.");
        }
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void update(String licenseType, String licenseNumber,
                       CounselorGender counselorGender, List<String> specializations,
                       List<String> approaches, String bio,
                       Integer experienceYears, String officeAddress) {
        if (licenseType != null)      this.licenseType = licenseType;
        if (licenseNumber != null)    this.licenseNumber = licenseNumber;
        if (counselorGender != null)  this.counselorGender = counselorGender;
        if (specializations != null)  this.specializations = specializations;
        if (approaches != null)       this.approaches = approaches;
        if (bio != null)              this.bio = bio;
        if (experienceYears != null)  this.experienceYears = experienceYears;
        if (officeAddress != null)    this.officeAddress = officeAddress;
    }

    public boolean isPending() {
        return approvalStatus == ApprovalStatus.PENDING;
    }

    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum CounselorGender {
        MALE, FEMALE, ANY
    }

    public void reapply() {
        if (this.approvalStatus != ApprovalStatus.REJECTED) {
            throw new IllegalStateException("반려된 상태에서만 재신청할 수 있습니다.");
        }
        this.approvalStatus = ApprovalStatus.PENDING;
        this.rejectionReason = null;
    }
}