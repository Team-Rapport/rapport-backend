package com.rapport.domain.booking.dto;

import com.rapport.domain.booking.entity.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingDto {

    @Getter
    public static class CreateRequest {
        @NotNull
        private Long counselorId;
        private Long sessionTypeId;    // 미입력 시 1 (CHAT)
        private LocalDate bookedDate;
        private LocalTime bookedStartTime;
        private LocalTime bookedEndTime;
        private String concern;
    }

    @Getter
    @Builder
    public static class BookingResponse {
        private Long bookingId;
        private Long clientId;
        private Long counselorId;
        private Booking.BookingStatus status;
        private LocalDate bookedDate;
        private LocalTime bookedStartTime;
        private LocalTime bookedEndTime;
        private String concern;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class ConfirmResponse {
        private Long bookingId;
        private Booking.BookingStatus status;
        private Long roomId;  // 자동 생성된 채팅방 ID
    }
}
