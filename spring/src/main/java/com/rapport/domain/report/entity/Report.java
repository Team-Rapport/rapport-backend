package com.rapport.domain.report.entity;

import com.rapport.domain.chat.entity.AiChatSession;
import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private AiChatSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(name = "depression_score", columnDefinition = "TINYINT UNSIGNED")
    private Integer depressionScore;

    @Column(name = "anxiety_score", columnDefinition = "TINYINT UNSIGNED")
    private Integer anxietyScore;

    @Column(name = "stress_score", columnDefinition = "TINYINT UNSIGNED")
    private Integer stressScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_keywords", columnDefinition = "json")
    private List<String> reportKeywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommended_specializations", columnDefinition = "json")
    private List<String> recommendedSpecializations;

    @Column(name = "is_crisis_detected", nullable = false)
    private boolean isCrisisDetected = false;

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

    // ===== 팩토리 =====
    public static Report create(AiChatSession session, User client,
                                 int depressionScore, int anxietyScore, int stressScore,
                                 RiskLevel riskLevel, String summary,
                                 List<String> keywords, List<String> specializations,
                                 boolean isCrisisDetected) {
        Report report = new Report();
        report.session = session;
        report.client = client;
        report.depressionScore = depressionScore;
        report.anxietyScore = anxietyScore;
        report.stressScore = stressScore;
        report.riskLevel = riskLevel;
        report.summary = summary;
        report.reportKeywords = keywords;
        report.recommendedSpecializations = specializations;
        report.isCrisisDetected = isCrisisDetected;
        return report;
    }

    public enum RiskLevel { LOW, MODERATE, HIGH, CRITICAL }
}
