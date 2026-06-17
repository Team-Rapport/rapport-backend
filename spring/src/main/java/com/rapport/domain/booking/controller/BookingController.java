package com.rapport.domain.booking.controller;

import com.rapport.domain.booking.dto.BookingDto;
import com.rapport.domain.booking.service.BookingService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Booking", description = "예약 생성·조회·취소 및 상담사 수락/거절 API")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    // ===== 공통 =====

    @Operation(summary = "상담사 가용 슬롯 조회")
    @GetMapping("/api/v1/counselors/{counselorId}/schedules")
    public ResponseEntity<ApiResponse<List<BookingDto.ScheduleSlotResponse>>> getAvailableSlots(
            @PathVariable Long counselorId) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getAvailableSlots(counselorId)));
    }

    // ===== 내담자 =====

    @Operation(summary = "예약 생성 (내담자)",
               description = "concern(주요 고민)은 선택 입력, reportId는 리포트 첨부 시에만 입력")
    @PostMapping("/api/v1/bookings")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookingDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "예약 요청이 완료되었습니다. 상담사 확정을 기다려주세요.",
                bookingService.createBooking(principal.getId(), request)));
    }

    @Operation(summary = "내 예약 목록 (내담자)")
    @GetMapping("/api/v1/my/bookings")
    public ResponseEntity<ApiResponse<Page<BookingDto.BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getClientBookings(principal.getId(), pageable)));
    }

    @Operation(summary = "예약 취소 (내담자)")
    @PatchMapping("/api/v1/my/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestBody(required = false) BookingDto.CancelRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.ok("예약이 취소되었습니다.",
                bookingService.cancelBooking(bookingId, principal.getId(), reason)));
    }

    // ===== 상담사 =====

    @Operation(summary = "예약 목록 (상담사)")
    @GetMapping("/api/v1/counselor/bookings")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<Page<BookingDto.BookingResponse>>> getCounselorBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getCounselorBookings(principal.getId(), pageable)));
    }

    @Operation(summary = "예약 수락 (상담사)", description = "PENDING → ACCEPTED. 내담자에게 확정 알림 발송.")
    @PatchMapping("/api/v1/counselor/bookings/{bookingId}/accept")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> acceptBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("예약을 수락했습니다.",
                bookingService.acceptBooking(bookingId, principal.getId())));
    }

    @Operation(summary = "예약 거절 (상담사)", description = "PENDING → REJECTED. 슬롯 복구.")
    @PatchMapping("/api/v1/counselor/bookings/{bookingId}/reject")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> rejectBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("예약을 거절했습니다.",
                bookingService.rejectBooking(bookingId, principal.getId())));
    }

    @Operation(summary = "예약 취소 (상담사)",
               description = "PENDING/ACCEPTED 상태 예약 취소. 슬롯 복구 및 내담자에게 알림 발송.")
    @PatchMapping("/api/v1/counselor/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> cancelBookingByCounselor(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestBody(required = false) BookingDto.CancelRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.ok("예약을 취소했습니다.",
                bookingService.cancelBookingByCounselor(bookingId, principal.getId(), reason)));
    }
}
