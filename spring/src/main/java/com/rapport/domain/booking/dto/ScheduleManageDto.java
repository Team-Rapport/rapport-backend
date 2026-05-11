package com.rapport.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ScheduleManageDto {

    // ===== 슬롯 단위 설정 =====

    @Getter
    public static class CreateSettingsRequest {
        @NotNull(message = "슬롯 단위를 입력해주세요.")
        @Min(value = 30, message = "슬롯 단위는 최소 30분입니다.")
        private Integer slotUnit;
    }

    @Getter
    @Builder
    public static class SettingsResponse {
        private Long counselorId;
        private int slotUnit;
    }

    // ===== 일정 일괄 생성 =====

    @Getter
    public static class BulkCreateRequest {
        @NotNull(message = "시작 날짜를 입력해주세요.")
        private LocalDate startDate;

        @NotNull(message = "종료 날짜를 입력해주세요.")
        private LocalDate endDate;

        @NotEmpty(message = "요일을 하나 이상 선택해주세요.")
        private List<DayOfWeek> daysOfWeek;

        @NotNull(message = "시작 시간을 입력해주세요.")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @NotNull(message = "종료 시간을 입력해주세요.")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        @NotNull(message = "상담 유형 ID를 입력해주세요.")
        private Long sessionTypeId;
    }

    @Getter
    @Builder
    public static class BulkCreateResult {
        private int createdCount;
        private int skippedCount;
    }

    // ===== 일정 단건 생성 =====

    @Getter
    public static class CreateScheduleRequest {
        @NotNull(message = "날짜를 입력해주세요.")
        private LocalDate slotDate;

        @NotNull(message = "시작 시간을 입력해주세요.")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @NotNull(message = "종료 시간을 입력해주세요.")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        @NotNull(message = "상담 유형 ID를 입력해주세요.")
        private Long sessionTypeId;
    }

    // ===== 브레이크타임 =====

    @Getter
    public static class CreateBreaktimeRequest {
        @NotNull(message = "브레이크타임 유형을 입력해주세요.")
        private BreaktimeType type;

        private LocalDate date;

        @NotNull(message = "시작 시간을 입력해주세요.")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @NotNull(message = "종료 시간을 입력해주세요.")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        public enum BreaktimeType { REGULAR, TEMPORARY }
    }

    // ===== 휴무일 =====

    @Getter
    public static class CreateDayoffRequest {
        @NotNull(message = "휴무 유형을 입력해주세요.")
        private DayoffType type;

        private List<DayOfWeek> daysOfWeek;

        private LocalDate date;

        public enum DayoffType { REGULAR_HOLIDAY, TEMPORARY_HOLIDAY }
    }

    // ===== 운영 종료 (날짜 전체 비활성화) =====

    @Getter
    public static class CloseScheduleRequest {
        @NotNull(message = "날짜를 입력해주세요.")
        private LocalDate date;
    }

    // ===== 일괄 삭제 =====

    @Getter
    public static class BulkDeleteRequest {
        @NotNull(message = "시작 날짜를 입력해주세요.")
        private LocalDate startDate;

        @NotNull(message = "종료 날짜를 입력해주세요.")
        private LocalDate endDate;
    }

    @Getter
    @Builder
    public static class BulkDeleteResult {
        private int deletedCount;
    }

    // ===== 응답: 슬롯 단건 =====

    @Getter
    @Builder
    public static class SlotResponse {
        private Long scheduleId;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        @JsonProperty("isAvailable")
        private boolean isAvailable;
    }

    // ===== 응답: 일간 스케줄 (오전/오후 구분) =====

    @Getter
    @Builder
    public static class DailyScheduleResponse {
        private LocalDate date;
        private int slotUnit;
        private List<SlotResponse> morning;
        private List<SlotResponse> afternoon;
    }

    // ===== 응답: 월간 캘린더 마킹용 =====

    @Getter
    @Builder
    public static class MonthlyScheduleResponse {
        private List<LocalDate> dates;
    }
}
