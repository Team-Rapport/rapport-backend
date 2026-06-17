package com.rapport.domain.counselor.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "counselor_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 20)
    private CredentialType credentialType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialStatus status = CredentialStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

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

    public static CounselorCredential create(User counselor, CredentialType type, String fileUrl) {
        CounselorCredential credential = new CounselorCredential();
        credential.counselor = counselor;
        credential.credentialType = type;
        credential.fileUrl = fileUrl;
        return credential;
    }

    public enum CredentialType {
        LICENSE,   // 자격증
        DEGREE,    // 학위 증명서
        CERT,      // 기타 수료증
        OTHER
    }

    public enum CredentialStatus {
        PENDING, APPROVED, REJECTED
    }
}
