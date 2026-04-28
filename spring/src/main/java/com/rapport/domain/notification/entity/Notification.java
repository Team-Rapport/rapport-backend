package com.rapport.domain.notification.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_via", nullable = false, length = 20)
    private SentVia sentVia = SentVia.IN_APP;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        sentAt    = LocalDateTime.now();
    }

    public static Notification create(User user, NotificationType type,
                                       String title, String body,
                                       String referenceType, Long referenceId) {
        Notification n = new Notification();
        n.user          = user;
        n.type          = type;
        n.title         = title;
        n.body          = body;
        n.referenceType = referenceType;
        n.referenceId   = referenceId;
        return n;
    }

    public void markRead() { this.isRead = true; }

    public enum NotificationType {
        BOOKING_REQ, CONFIRMED, CANCELLED, INTAKE_REQ,
        REMINDER, CHAT, REPORT, SYSTEM,
        COUNSELOR_APPROVED, COUNSELOR_REJECTED
    }

    public enum SentVia { IN_APP, EMAIL, KAKAO }
}
