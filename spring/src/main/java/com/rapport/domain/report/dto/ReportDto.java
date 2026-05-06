package com.rapport.domain.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapport.domain.report.entity.Report;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

public class ReportDto {

    // FastAPI 내부 서버 → Spring: X-Service-Key 헤더 인증, userId 포함
    @Getter
    public static class InternalSaveRequest {
        @NotNull private Long userId;
        @NotNull private Long sessionId;
        @NotNull @Min(0) @Max(100) private Integer depressionScore;
        @NotNull @Min(0) @Max(100) private Integer anxietyScore;
        @NotNull @Min(0) @Max(100) private Integer stressScore;
        @NotNull private Report.RiskLevel riskLevel;
        private String summary;
        private List<String> reportKeywords;
        private List<String> recommendedSpecializations;
        @JsonProperty("isCrisisDetected")
        private boolean crisisDetected;
    }

    // FastAPI → Spring: 리포트 저장 요청
    @Getter
    public static class SaveRequest {
        @NotNull private Long sessionId;
        @NotNull @Min(0) @Max(100) private Integer depressionScore;
        @NotNull @Min(0) @Max(100) private Integer anxietyScore;
        @NotNull @Min(0) @Max(100) private Integer stressScore;
        @NotNull private Report.RiskLevel riskLevel;
        private String summary;
        private List<String> reportKeywords;
        private List<String> recommendedSpecializations;
        @JsonProperty("isCrisisDetected")
        private boolean crisisDetected;
    }

    // 리포트 목록 (카드용 요약)
    @Getter
    @Builder
    public static class ReportSummary {
        private Long reportId;
        private Long sessionId;
        private Integer depressionScore;
        private Integer anxietyScore;
        private Integer stressScore;
        private Report.RiskLevel riskLevel;
        @JsonProperty("isCrisisDetected")
        private boolean crisisDetected;
        private LocalDateTime createdAt;
    }

    // 리포트 상세
    @Getter
    @Builder
    public static class ReportDetail {
        private Long reportId;
        private Long sessionId;
        private Integer depressionScore;
        private Integer anxietyScore;
        private Integer stressScore;
        private Report.RiskLevel riskLevel;
        private String summary;
        private List<String> reportKeywords;
        private List<String> recommendedSpecializations;
        @JsonProperty("isCrisisDetected")
        private boolean crisisDetected;
        private LocalDateTime createdAt;
    }
}
