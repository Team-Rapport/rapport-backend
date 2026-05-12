package com.rapport.domain.booking.controller;

import com.rapport.domain.booking.dto.ScheduleManageDto;
import com.rapport.domain.booking.service.CounselorScheduleService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Counselor Schedule Management", description = "상담사 예약 슬롯 관리 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('COUNSELOR')")
@SecurityRequirement(name = "bearerAuth")
public class CounselorScheduleManageController {

    private final CounselorScheduleService scheduleService;

    // ===== 슬롯 단위 설정 =====

    @Operation(summary = "슬롯 단위 설정 (최초 1회)",
               description = "30 또는 60분 설정. 변경은 PATCH 사용.")
    @PostMapping("/api/v1/counselor/schedule/settings")
    public ResponseEntity<ApiResponse<ScheduleManageDto.SettingsResponse>> createSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.CreateSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("슬롯 설정이 완료되었습니다.",
                scheduleService.createSettings(principal.getId(), request)));
    }

    @Operation(summary = "슬롯 단위 변경 (30 ↔ 60분)",
               description = "마지막 예약일 다음 날부터 새 slotUnit 적용. 해당 시점 이후 슬롯은 자동 삭제됩니다.")
    @PatchMapping("/api/v1/counselor/schedule/settings")
    public ResponseEntity<ApiResponse<ScheduleManageDto.UpdateSettingsResponse>> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.UpdateSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("슬롯 단위가 변경되었습니다.",
                scheduleService.updateSettings(principal.getId(), request)));
    }

    // ===== 일정 생성 =====

    @Operation(summary = "일정 일괄 생성",
               description = "날짜 범위·요일·시간대로 슬롯 자동 생성. 휴무일·브레이크타임 자동 제외.")
    @PostMapping("/api/v1/counselor/schedules/bulk")
    public ResponseEntity<ApiResponse<ScheduleManageDto.BulkCreateResult>> bulkCreateSchedules(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.BulkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("일정이 생성되었습니다.",
                scheduleService.bulkCreateSchedules(principal.getId(), request)));
    }

    @Operation(summary = "일정 단건 생성")
    @PostMapping("/api/v1/counselor/schedules")
    public ResponseEntity<ApiResponse<ScheduleManageDto.SlotResponse>> createSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.CreateScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("일정이 생성되었습니다.",
                scheduleService.createSchedule(principal.getId(), request)));
    }

    // ===== 브레이크타임 / 휴무일 =====

    @Operation(summary = "브레이크타임 등록",
               description = "정기(REGULAR): 매일 적용. 개별(TEMPORARY): 특정 날짜 적용.")
    @PostMapping("/api/v1/counselor/schedules/breaktime")
    public ResponseEntity<ApiResponse<Void>> createBreaktime(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.CreateBreaktimeRequest request) {
        scheduleService.createBreaktime(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("브레이크타임이 등록되었습니다."));
    }

    @Operation(summary = "브레이크타임 삭제")
    @DeleteMapping("/api/v1/counselor/schedules/breaktime/{dayoffId}")
    public ResponseEntity<ApiResponse<Void>> deleteBreaktime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dayoffId) {
        scheduleService.deleteBreaktime(principal.getId(), dayoffId);
        return ResponseEntity.ok(ApiResponse.ok("브레이크타임이 삭제되었습니다."));
    }

    @Operation(summary = "휴무일 등록",
               description = "정기(REGULAR_HOLIDAY): 요일 기반 반복. 임시(TEMPORARY_HOLIDAY): 특정 날짜 1회.")
    @PostMapping("/api/v1/counselor/schedules/dayoff")
    public ResponseEntity<ApiResponse<Void>> createDayoff(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.CreateDayoffRequest request) {
        scheduleService.createDayoff(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("휴무일이 등록되었습니다."));
    }

    @Operation(summary = "휴무일 삭제")
    @DeleteMapping("/api/v1/counselor/schedules/dayoff/{dayoffId}")
    public ResponseEntity<ApiResponse<Void>> deleteDayoff(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dayoffId) {
        scheduleService.deleteDayoff(principal.getId(), dayoffId);
        return ResponseEntity.ok(ApiResponse.ok("휴무일이 삭제되었습니다."));
    }

    // ===== 비활성화 / 삭제 =====

    @Operation(summary = "슬롯 단건 활성화",
               description = "특정 슬롯을 is_available=true 처리 (재오픈).")
    @PatchMapping("/api/v1/counselor/schedules/{scheduleId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId) {
        scheduleService.activateSchedule(principal.getId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("슬롯이 활성화되었습니다."));
    }

    @Operation(summary = "슬롯 단건 비활성화",
               description = "특정 슬롯을 is_available=false 처리. PENDING/ACCEPTED 예약 있으면 실패.")
    @PatchMapping("/api/v1/counselor/schedules/{scheduleId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId) {
        scheduleService.deactivateSchedule(principal.getId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("슬롯이 비활성화되었습니다."));
    }

    @Operation(summary = "날짜 운영 종료",
               description = "해당 날짜의 모든 슬롯을 is_available=false 처리. PENDING/ACCEPTED 예약 있으면 실패.")
    @PatchMapping("/api/v1/counselor/schedules/close")
    public ResponseEntity<ApiResponse<Void>> closeSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.CloseScheduleRequest request) {
        scheduleService.closeSchedule(principal.getId(), request.getDate());
        return ResponseEntity.ok(ApiResponse.ok("해당 날짜의 슬롯이 비활성화되었습니다."));
    }

    @Operation(summary = "일정 단건 삭제",
               description = "PENDING/ACCEPTED 예약이 있으면 삭제 불가.")
    @DeleteMapping("/api/v1/counselor/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(principal.getId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("일정이 삭제되었습니다."));
    }

    @Operation(summary = "일정 일괄 삭제",
               description = "날짜 범위 내 예약 없는 슬롯만 삭제. 예약 있는 슬롯은 유지.")
    @DeleteMapping("/api/v1/counselor/schedules/bulk")
    public ResponseEntity<ApiResponse<ScheduleManageDto.BulkDeleteResult>> bulkDeleteSchedules(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleManageDto.BulkDeleteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("예약 없는 슬롯이 삭제되었습니다.",
                scheduleService.bulkDeleteSchedules(principal.getId(),
                        request.getStartDate(), request.getEndDate())));
    }

    // ===== 조회 =====

    @Operation(summary = "브레이크타임 목록 조회")
    @GetMapping("/api/v1/counselor/schedules/breaktimes")
    public ResponseEntity<ApiResponse<List<ScheduleManageDto.DayoffResponse>>> getBreaktimes(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getBreaktimes(principal.getId())));
    }

    @Operation(summary = "휴무일 목록 조회", description = "정기 휴무일 목록 반환. 임시 휴무일은 /dayoffs/temporary 사용.")
    @GetMapping("/api/v1/counselor/schedules/dayoffs")
    public ResponseEntity<ApiResponse<List<ScheduleManageDto.DayoffResponse>>> getDayoffs(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getDayoffs(principal.getId())));
    }

    @Operation(summary = "임시 휴무일 목록 조회")
    @GetMapping("/api/v1/counselor/schedules/dayoffs/temporary")
    public ResponseEntity<ApiResponse<List<ScheduleManageDto.DayoffResponse>>> getTemporaryDayoffs(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getTemporaryDayoffs(principal.getId())));
    }

    @Operation(summary = "월별 슬롯 있는 날짜 목록",
               description = "캘린더 마킹용. 해당 월에 슬롯이 존재하는 날짜 목록 반환.")
    @GetMapping("/api/v1/counselor/schedules")
    public ResponseEntity<ApiResponse<ScheduleManageDto.MonthlyScheduleResponse>> getMonthlySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getMonthlyDates(principal.getId(), month)));
    }

    @Operation(summary = "일별 슬롯 목록 (오전/오후 구분)",
               description = "오전: 06:00~11:59, 오후: 12:00~20:59")
    @GetMapping("/api/v1/counselor/schedules/daily")
    public ResponseEntity<ApiResponse<ScheduleManageDto.DailyScheduleResponse>> getDailySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getDailySchedule(principal.getId(), date)));
    }

    @Operation(summary = "일별 통합 뷰 (슬롯 + 예약 정보)",
               description = "각 슬롯에 PENDING/ACCEPTED 예약 정보를 포함해 반환. 타임테이블 렌더링용.")
    @GetMapping("/api/v1/counselor/schedules/daily/integrated")
    public ResponseEntity<ApiResponse<ScheduleManageDto.DailyIntegratedResponse>> getDailyIntegrated(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.getDailyScheduleWithBookings(principal.getId(), date)));
    }
}
