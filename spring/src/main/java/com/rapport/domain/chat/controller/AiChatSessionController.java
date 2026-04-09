package com.rapport.domain.chat.controller;

import com.rapport.domain.chat.dto.AiChatSessionDto;
import com.rapport.domain.chat.service.AiChatSessionService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Chat Session", description = "AI 챗봇 세션 관리 API")
@RestController
@RequestMapping("/api/v1/chat/sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AiChatSessionController {

    private final AiChatSessionService sessionService;

    @Operation(summary = "챗봇 세션 시작", description = "면책 고지 동의 후 새 세션을 시작합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AiChatSessionDto.SessionResponse>> startSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiChatSessionDto.StartRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("세션이 시작되었습니다.",
                sessionService.startSession(principal.getId(), request.getConsentAgreed())));
    }

    @Operation(summary = "챗봇 세션 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AiChatSessionDto.SessionSummary>>> getMySessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                sessionService.getMySessions(principal.getId(), pageable)));
    }

    @Operation(summary = "챗봇 세션 단건 조회")
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<AiChatSessionDto.SessionResponse>> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                sessionService.getSession(sessionId, principal.getId())));
    }

    @Operation(summary = "챗봇 세션 완료", description = "FastAPI 리포트 생성 완료 후 호출합니다.")
    @PatchMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<AiChatSessionDto.SessionResponse>> completeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok("세션이 완료되었습니다.",
                sessionService.completeSession(sessionId, principal.getId())));
    }

    @Operation(summary = "챗봇 세션 포기")
    @PatchMapping("/{sessionId}/abandon")
    public ResponseEntity<ApiResponse<Void>> abandonSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId) {
        sessionService.abandonSession(sessionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("세션이 종료되었습니다."));
    }
}
