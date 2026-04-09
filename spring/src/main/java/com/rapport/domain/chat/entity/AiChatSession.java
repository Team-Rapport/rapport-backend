package com.rapport.domain.chat.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "consent_agreed", nullable = false)
    private boolean consentAgreed = false;

    @Column(name = "consent_at")
    private LocalDateTime consentAt;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static AiChatSession start(User client, boolean consentAgreed) {
        AiChatSession session = new AiChatSession();
        session.client = client;
        session.consentAgreed = consentAgreed;
        if (consentAgreed) session.consentAt = LocalDateTime.now();
        return session;
    }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.finishedAt = LocalDateTime.now();
    }

    public void abandon() {
        this.status = SessionStatus.ABANDONED;
        this.finishedAt = LocalDateTime.now();
    }

    public boolean isInProgress() { return status == SessionStatus.IN_PROGRESS; }

    public enum SessionStatus { IN_PROGRESS, COMPLETED, ABANDONED }
}
