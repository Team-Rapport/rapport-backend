package com.rapport.domain.intakeform.controller;

import com.rapport.domain.intakeform.dto.IntakeFormDto;
import com.rapport.domain.intakeform.service.IntakeFormService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "IntakeForm (상담사)", description = "접수면접지 요청·열람 API (상담사)")
@RestController
@RequestMapping("/api/v1/counselor/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('COUNSELOR')")
public class CounselorIntakeFormController {

    private final IntakeFormService intakeFormService;

    @Operation(summary = "접수면접지 작성 요청 발송 (상담사)",
               description = "채팅방에 요청 메시지를 발송하고 내담자에게 알림을 전송합니다.")
    @PostMapping("/{bookingId}/intake-form/request")
    public ResponseEntity<ApiResponse<IntakeFormDto.RequestResponse>> requestIntakeForm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "접수면접지 작성 요청이 발송되었습니다.",
                intakeFormService.requestIntakeForm(principal.getId(), bookingId)));
    }

    @Operation(summary = "내담자 접수면접지 열람 (상담사)",
               description = "제출된 접수면접지가 없으면 404를 반환합니다.")
    @GetMapping("/{bookingId}/intake-form")
    public ResponseEntity<ApiResponse<IntakeFormDto.IntakeFormResponse>> getIntakeForm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                intakeFormService.getCounselorIntakeForm(principal.getId(), bookingId)));
    }
}
