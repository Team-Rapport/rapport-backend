package com.rapport.domain.chat.dto;

import com.rapport.domain.chat.entity.AiChatSession;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

public class AiChatSessionDto {

    @Getter
    public static class StartRequest {
        @NotNull(message = "면책 고지 동의 여부를 선택해주세요.")
        private Boolean consentAgreed;
    }

    @Getter
    @Builder
    public static class SessionResponse {
        private Long sessionId;
        private AiChatSession.SessionStatus status;
        private boolean consentAgreed;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }

    @Getter
    @Builder
    public static class SessionSummary {
        private Long sessionId;
        private AiChatSession.SessionStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private boolean hasReport; // 리포트 생성 여부
    }
}
