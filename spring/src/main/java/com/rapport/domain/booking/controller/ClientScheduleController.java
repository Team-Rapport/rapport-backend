package com.rapport.domain.booking.controller;

import com.rapport.domain.booking.dto.ScheduleManageDto;
import com.rapport.domain.booking.service.CounselorScheduleService;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// /api/v1/counselors/** 는 SecurityConfig PUBLIC_PATHS에 이미 포함되어 있어 인증 불필요
@Tag(name = "Client Schedule", description = "내담자용 상담사 가용 슬롯 조회 API (공개)")
@RestController
@RequiredArgsConstructor
public class ClientScheduleController {

    private final CounselorScheduleService scheduleService;

    @Operation(summary = "상담사 가용 슬롯 조회 (내담자용)",
               description = "isAvailable=true 슬롯만 오전(06:00~11:59)/오후(12:00~20:59) 구분 반환.")
    @GetMapping("/api/v1/counselors/{counselorId}/schedules/available")
    public ResponseEntity<ApiResponse<ScheduleManageDto.DailyScheduleResponse>> getAvailableSchedule(
            @PathVariable Long counselorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getAvailableSchedule(counselorId, date)));
    }
}
