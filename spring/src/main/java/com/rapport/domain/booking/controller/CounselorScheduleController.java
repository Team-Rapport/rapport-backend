package com.rapport.domain.booking.controller;

import com.rapport.domain.booking.dto.BookingDto;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.booking.service.BookingService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Counselor Schedule", description = "상담사 일정 관리 API (캘린더)")
@RestController
@RequestMapping("/api/v1/counselor/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COUNSELOR')")
@SecurityRequirement(name = "bearerAuth")
public class CounselorScheduleController {

    private final BookingRepository bookingRepository;
    private final BookingService    bookingService;

    /**
     * 특정 날짜의 예약 목록 (시간순 정렬)
     * 캘린더에서 날짜 클릭 시 호출
     */
    @Operation(summary = "날짜별 예약 조회",
               description = "선택한 날짜의 확정된 예약을 시간순으로 반환합니다.")
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<BookingDto.BookingResponse>>> getDailySchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<BookingDto.BookingResponse> schedule =
                bookingRepository.findDailyByCounselor(principal.getId(), date)
                        .stream()
                        .map(bookingService::toResponse)
                        .toList();

        return ResponseEntity.ok(ApiResponse.ok(schedule));
    }

    /**
     * 월별 예약 있는 날짜 목록 (캘린더 마킹용)
     * 캘린더 월 이동 시 호출 → 예약 있는 날짜에 점 표시
     */
    @Operation(summary = "월별 예약 날짜 목록",
               description = "해당 월에 예약이 있는 날짜 목록을 반환합니다. 캘린더 마킹에 사용.")
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getMonthlyBookedDates(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

        LocalDate start = month.atDay(1);
        LocalDate end   = month.atEndOfMonth();

        List<LocalDate> bookedDates =
                bookingRepository.findBookedDatesByCounselorAndMonth(
                                principal.getId(), start, end)
                        .stream()
                        .distinct()
                        .sorted()
                        .toList();

        return ResponseEntity.ok(ApiResponse.ok(bookedDates));
    }

}
