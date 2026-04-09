package com.rapport.domain.report.service;

import com.rapport.domain.chat.entity.AiChatSession;
import com.rapport.domain.chat.entity.AiChatSessionRepository;
import com.rapport.domain.report.dto.ReportDto;
import com.rapport.domain.report.entity.Report;
import com.rapport.domain.report.entity.ReportRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final AiChatSessionRepository sessionRepository;
    private final UserRepository userRepository;

    // ===== 리포트 저장 (FastAPI 호출용) =====
    @Transactional
    public ReportDto.ReportDetail saveReport(Long clientId, ReportDto.SaveRequest request) {
        AiChatSession session = sessionRepository.findByIdAndClientId(
                        request.getSessionId(), clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (reportRepository.existsBySessionId(session.getId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 리포트가 생성된 세션입니다.");
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Report report = Report.create(
                session, client,
                request.getDepressionScore(), request.getAnxietyScore(), request.getStressScore(),
                request.getRiskLevel(), request.getSummary(),
                request.getReportKeywords(), request.getRecommendedSpecializations(),
                request.isCrisisDetected()
        );
        reportRepository.save(report);

        // 세션 완료 처리
        session.complete();

        log.info("Report saved: reportId={}, clientId={}, riskLevel={}",
                report.getId(), clientId, report.getRiskLevel());
        return toDetail(report);
    }

    // ===== 내 리포트 목록 =====
    @Transactional(readOnly = true)
    public Page<ReportDto.ReportSummary> getMyReports(Long clientId, Pageable pageable) {
        return reportRepository.findAllByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toSummary);
    }

    // ===== 리포트 상세 조회 =====
    @Transactional(readOnly = true)
    public ReportDto.ReportDetail getReport(Long reportId, Long clientId) {
        Report report = reportRepository.findByIdAndClientId(reportId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toDetail(report);
    }

    // ===== 세션으로 리포트 조회 =====
    @Transactional(readOnly = true)
    public ReportDto.ReportDetail getReportBySession(Long sessionId, Long clientId) {
        sessionRepository.findByIdAndClientId(sessionId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toDetail(report);
    }

    private ReportDto.ReportSummary toSummary(Report r) {
        return ReportDto.ReportSummary.builder()
                .reportId(r.getId())
                .sessionId(r.getSession().getId())
                .depressionScore(r.getDepressionScore())
                .anxietyScore(r.getAnxietyScore())
                .stressScore(r.getStressScore())
                .riskLevel(r.getRiskLevel())
                .isCrisisDetected(r.isCrisisDetected())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReportDto.ReportDetail toDetail(Report r) {
        return ReportDto.ReportDetail.builder()
                .reportId(r.getId())
                .sessionId(r.getSession().getId())
                .depressionScore(r.getDepressionScore())
                .anxietyScore(r.getAnxietyScore())
                .stressScore(r.getStressScore())
                .riskLevel(r.getRiskLevel())
                .summary(r.getSummary())
                .reportKeywords(r.getReportKeywords())
                .recommendedSpecializations(r.getRecommendedSpecializations())
                .isCrisisDetected(r.isCrisisDetected())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
