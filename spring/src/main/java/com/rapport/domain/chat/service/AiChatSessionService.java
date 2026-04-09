package com.rapport.domain.chat.service;

import com.rapport.domain.chat.dto.AiChatSessionDto;
import com.rapport.domain.chat.entity.AiChatSession;
import com.rapport.domain.chat.entity.AiChatSessionRepository;
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
public class AiChatSessionService {

    private final AiChatSessionRepository sessionRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    // ===== 세션 시작 =====
    @Transactional
    public AiChatSessionDto.SessionResponse startSession(Long clientId, boolean consentAgreed) {
        if (!consentAgreed) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "AI 챗봇 이용을 위해 면책 고지에 동의해야 합니다.");
        }
        // 진행 중인 세션이 있으면 먼저 종료
        sessionRepository.findTopByClientIdAndStatusOrderByCreatedAtDesc(
                clientId, AiChatSession.SessionStatus.IN_PROGRESS)
                .ifPresent(AiChatSession::abandon);

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AiChatSession session = AiChatSession.start(client, true);
        session = sessionRepository.save(session);
        log.info("AI chat session started: sessionId={}, clientId={}", session.getId(), clientId);
        return toResponse(session);
    }

    // ===== 세션 완료 (FastAPI가 리포트 생성 후 호출) =====
    @Transactional
    public AiChatSessionDto.SessionResponse completeSession(Long sessionId, Long clientId) {
        AiChatSession session = getMySession(sessionId, clientId);
        if (!session.isInProgress()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "진행 중인 세션이 아닙니다.");
        }
        session.complete();
        log.info("AI chat session completed: sessionId={}", sessionId);
        return toResponse(session);
    }

    // ===== 세션 포기 =====
    @Transactional
    public void abandonSession(Long sessionId, Long clientId) {
        AiChatSession session = getMySession(sessionId, clientId);
        session.abandon();
        log.info("AI chat session abandoned: sessionId={}", sessionId);
    }

    // ===== 세션 목록 조회 =====
    @Transactional(readOnly = true)
    public Page<AiChatSessionDto.SessionSummary> getMySessions(Long clientId, Pageable pageable) {
        return sessionRepository.findAllByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(session -> AiChatSessionDto.SessionSummary.builder()
                        .sessionId(session.getId())
                        .status(session.getStatus())
                        .startedAt(session.getStartedAt())
                        .finishedAt(session.getFinishedAt())
                        .hasReport(reportRepository.existsBySessionId(session.getId()))
                        .build());
    }

    // ===== 세션 단건 조회 =====
    @Transactional(readOnly = true)
    public AiChatSessionDto.SessionResponse getSession(Long sessionId, Long clientId) {
        return toResponse(getMySession(sessionId, clientId));
    }

    private AiChatSession getMySession(Long sessionId, Long clientId) {
        return sessionRepository.findByIdAndClientId(sessionId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AiChatSessionDto.SessionResponse toResponse(AiChatSession session) {
        return AiChatSessionDto.SessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .consentAgreed(session.isConsentAgreed())
                .startedAt(session.getStartedAt())
                .finishedAt(session.getFinishedAt())
                .build();
    }
}
