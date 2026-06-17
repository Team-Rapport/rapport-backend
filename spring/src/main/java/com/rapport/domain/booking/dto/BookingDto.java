package com.rapport.domain.booking.dto;

import com.rapport.domain.booking.entity.Booking;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingDto {

    // ===== 내담자: 예약 생성 요청 =====
    @Getter
    public static class CreateRequest {
        @NotNull(message = "상담사 ID를 입력해주세요.")
        private Long counselorId;

        @NotNull(message = "스케줄 ID를 입력해주세요.")
        private Long scheduleId;

        @NotNull(message = "상담 유형 ID를 입력해주세요.")
        private Long sessionTypeId;

        private Long reportId; // 선택 첨부

        @Size(max = 300, message = "주요 고민은 300자 이내로 입력해주세요.")
        private String concern; // ★ V2 추가: 예약 시 주요 고민 입력 (선택)
    }

    // ===== 내담자: 예약 취소 요청 =====
    @Getter
    public static class CancelRequest {
        private String reason;
    }

    // ===== 예약 응답 (목록/상세 공통) =====
    @Getter
    @Builder
    public static class BookingResponse {
        private Long bookingId;
        private Booking.BookingStatus status;
        // 상담사 정보
        private Long counselorId;
        private String counselorName;
        // 내담자 정보
        private Long clientId;
        private String clientName;
        // 일정
        private LocalDate bookedDate;
        private LocalTime bookedStartTime;
        private LocalTime bookedEndTime;
        // 상담 유형
        private String sessionTypeName;
        // 주요 고민 ★ V2 추가
        private String concern;
        // 취소 정보
        private String cancellationReason;
        private Booking.CancelledBy cancelledBy;
        private LocalDateTime cancelledAt;
        private LocalDateTime createdAt;
    }

    // ===== 가용 슬롯 응답 =====
    @Getter
    @Builder
    public static class ScheduleSlotResponse {
        private Long scheduleId;
        private LocalDate slotDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String sessionTypeName;
    }
}
