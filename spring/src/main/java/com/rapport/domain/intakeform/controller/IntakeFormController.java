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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "IntakeForm (내담자)", description = "접수면접지 작성·수정·조회 API (내담자)")
@RestController
@RequestMapping("/api/v1/intake-forms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class IntakeFormController {

    private final IntakeFormService intakeFormService;

    @Operation(summary = "접수면접지 작성 및 제출 (내담자)",
               description = "예약 확정(ACCEPTED) 상태에서, 상담일 자정 이전까지 작성 가능합니다.")
    @PostMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<IntakeFormDto.IntakeFormResponse>> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestBody IntakeFormDto.SubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "접수면접지가 제출되었습니다.",
                intakeFormService.submitIntakeForm(principal.getId(), bookingId, request)));
    }

    @Operation(summary = "접수면접지 수정 (내담자)",
               description = "null 필드는 기존값을 유지합니다. 상담일 자정 이전까지 수정 가능합니다.")
    @PatchMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<IntakeFormDto.IntakeFormResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestBody IntakeFormDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "접수면접지가 수정되었습니다.",
                intakeFormService.updateIntakeForm(principal.getId(), bookingId, request)));
    }

    @Operation(summary = "내 접수면접지 조회 (내담자)")
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<IntakeFormDto.IntakeFormResponse>> getMyIntakeForm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                intakeFormService.getMyIntakeForm(principal.getId(), bookingId)));
    }
}
