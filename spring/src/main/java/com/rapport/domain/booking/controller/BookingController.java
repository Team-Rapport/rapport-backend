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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Booking", description = "예약 API")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "예약 생성", description = "내담자가 상담사에게 예약을 요청합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookingDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("예약이 요청되었습니다.",
                bookingService.createBooking(principal.getId(), request)));
    }

    @Operation(summary = "예약 확정 + 채팅방 자동 생성", description = "상담사가 예약을 확정하면 채팅방이 자동으로 생성됩니다.")
    @PatchMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingDto.ConfirmResponse>> confirmBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("예약이 확정되고 채팅방이 생성되었습니다.",
                bookingService.confirmBooking(bookingId, principal.getId())));
    }

    @Operation(summary = "내 예약 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingDto.BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getMyBookings(principal.getId())));
    }
}
