package com.rapport.domain.booking.service;

import com.rapport.domain.booking.dto.ScheduleManageDto;
import com.rapport.domain.booking.entity.*;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static com.rapport.domain.booking.entity.CounselorDayoff.DayoffType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselorScheduleService {

    private static final LocalTime MORNING_START  = LocalTime.of(6, 0);
    private static final LocalTime MORNING_END    = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_END  = LocalTime.of(21, 0);

    private static final List<Booking.BookingStatus> ACTIVE_STATUSES =
            List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.ACCEPTED);

    private final CounselorScheduleSettingsRepository settingsRepository;
    private final CounselorDayoffRepository dayoffRepository;
    private final CounselorScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final UserRepository userRepository;

    // ===== 슬롯 단위 설정 =====

    @Transactional
    public ScheduleManageDto.SettingsResponse createSettings(Long counselorId,
                                                              ScheduleManageDto.CreateSettingsRequest req) {
        if (settingsRepository.existsByCounselorId(counselorId)) {
            throw new BusinessException(ErrorCode.SCHEDULE_SETTINGS_ALREADY_EXISTS);
        }
        User counselor = findUserOrThrow(counselorId);
        CounselorScheduleSettings settings = CounselorScheduleSettings.create(counselor, req.getSlotUnit());
        settingsRepository.save(settings);
        log.info("Schedule settings created: counselorId={}, slotUnit={}", counselorId, req.getSlotUnit());
        return ScheduleManageDto.SettingsResponse.builder()
                .counselorId(counselorId)
                .slotUnit(settings.getSlotUnit())
                .build();
    }

    // ===== 일정 단건 생성 =====

    @Transactional
    public ScheduleManageDto.SlotResponse createSchedule(Long counselorId,
                                                          ScheduleManageDto.CreateScheduleRequest req) {
        validateFutureDate(req.getSlotDate());
        User counselor = findUserOrThrow(counselorId);
        SessionType sessionType = findSessionTypeOrThrow(req.getSessionTypeId());
        CounselorSchedule schedule = CounselorSchedule.create(
                counselor, sessionType, req.getSlotDate(), req.getStartTime(), req.getEndTime());
        scheduleRepository.save(schedule);
        log.info("Schedule created: counselorId={}, date={}", counselorId, req.getSlotDate());
        return toSlotResponse(schedule);
    }

    // ===== 일정 일괄 생성 =====

    @Transactional
    public ScheduleManageDto.BulkCreateResult bulkCreateSchedules(Long counselorId,
                                                                    ScheduleManageDto.BulkCreateRequest req) {
        CounselorScheduleSettings settings = settingsRepository.findByCounselorId(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_SETTINGS_NOT_FOUND));
        User counselor = findUserOrThrow(counselorId);
        SessionType sessionType = findSessionTypeOrThrow(req.getSessionTypeId());
        int slotUnit = settings.getSlotUnit();

        Set<DayOfWeek> regularHolidayDays = dayoffRepository
                .findByCounselorIdAndDayoffType(counselorId, REGULAR_HOLIDAY).stream()
                .map(CounselorDayoff::getDayOfWeek)
                .collect(Collectors.toSet());

        Set<LocalDate> temporaryHolidayDates = dayoffRepository
                .findByCounselorIdAndDayoffType(counselorId, TEMPORARY_HOLIDAY).stream()
                .map(CounselorDayoff::getDayoffDate)
                .collect(Collectors.toSet());

        List<CounselorDayoff> regularBreaktimes = dayoffRepository
                .findByCounselorIdAndDayoffTypeAndDayoffDateIsNull(counselorId, BREAKTIME);

        Set<String> existingSlotKeys = scheduleRepository
                .findByCounselorIdAndSlotDateBetween(counselorId, req.getStartDate(), req.getEndDate())
                .stream()
                .map(s -> s.getSlotDate() + "T" + s.getStartTime())
                .collect(Collectors.toSet());

        Set<DayOfWeek> requestedDays = new HashSet<>(req.getDaysOfWeek());
        LocalDate today = LocalDate.now();
        List<CounselorSchedule> toSave = new ArrayList<>();
        int skipped = 0;

        for (LocalDate date = req.getStartDate(); !date.isAfter(req.getEndDate()); date = date.plusDays(1)) {
            if (date.isBefore(today)) { skipped++; continue; }
            if (!requestedDays.contains(date.getDayOfWeek())) continue;
            if (regularHolidayDays.contains(date.getDayOfWeek())) { skipped++; continue; }
            if (temporaryHolidayDates.contains(date)) { skipped++; continue; }

            List<CounselorDayoff> dateBreaktimes =
                    dayoffRepository.findByCounselorIdAndDayoffTypeAndDayoffDate(counselorId, BREAKTIME, date);

            List<CounselorDayoff> allBreaktimes = new ArrayList<>(regularBreaktimes);
            allBreaktimes.addAll(dateBreaktimes);

            LocalTime current = req.getStartTime();
            while (!current.plusMinutes(slotUnit).isAfter(req.getEndTime())) {
                LocalTime slotEnd = current.plusMinutes(slotUnit);
                String key = date + "T" + current;
                if (!overlapsBreaktimes(current, slotEnd, allBreaktimes) && !existingSlotKeys.contains(key)) {
                    toSave.add(CounselorSchedule.create(counselor, sessionType, date, current, slotEnd));
                    existingSlotKeys.add(key);
                } else if (existingSlotKeys.contains(key)) {
                    skipped++;
                }
                current = slotEnd;
            }
        }

        scheduleRepository.saveAll(toSave);
        log.info("Bulk schedules created: counselorId={}, count={}, skipped={}",
                counselorId, toSave.size(), skipped);
        return ScheduleManageDto.BulkCreateResult.builder()
                .createdCount(toSave.size())
                .skippedCount(skipped)
                .build();
    }

    // ===== 브레이크타임 등록 =====

    @Transactional
    public void createBreaktime(Long counselorId, ScheduleManageDto.CreateBreaktimeRequest req) {
        User counselor = findUserOrThrow(counselorId);
        CounselorDayoff dayoff = switch (req.getType()) {
            case REGULAR -> CounselorDayoff.createRegularBreaktime(
                    counselor, req.getStartTime(), req.getEndTime());
            case TEMPORARY -> {
                if (req.getDate() == null) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "개별 브레이크타임은 날짜가 필요합니다.");
                }
                validateFutureDate(req.getDate());
                yield CounselorDayoff.createTemporaryBreaktime(
                        counselor, req.getDate(), req.getStartTime(), req.getEndTime());
            }
        };
        dayoffRepository.save(dayoff);
        log.info("Breaktime created: counselorId={}, type={}", counselorId, req.getType());
    }

    // ===== 휴무일 등록 =====

    @Transactional
    public void createDayoff(Long counselorId, ScheduleManageDto.CreateDayoffRequest req) {
        User counselor = findUserOrThrow(counselorId);
        switch (req.getType()) {
            case REGULAR_HOLIDAY -> {
                if (req.getDaysOfWeek() == null || req.getDaysOfWeek().isEmpty()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "정기 휴무일은 요일 선택이 필요합니다.");
                }
                List<CounselorDayoff> dayoffs = req.getDaysOfWeek().stream()
                        .map(day -> CounselorDayoff.createRegularHoliday(counselor, day))
                        .toList();
                dayoffRepository.saveAll(dayoffs);
            }
            case TEMPORARY_HOLIDAY -> {
                if (req.getDate() == null) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "임시 휴무일은 날짜가 필요합니다.");
                }
                validateFutureDate(req.getDate());
                dayoffRepository.save(CounselorDayoff.createTemporaryHoliday(counselor, req.getDate()));
            }
        }
        log.info("Dayoff created: counselorId={}, type={}", counselorId, req.getType());
    }

    // ===== 브레이크타임 삭제 =====

    @Transactional
    public void deleteBreaktime(Long counselorId, Long dayoffId) {
        CounselorDayoff dayoff = dayoffRepository.findByIdAndCounselorId(dayoffId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAYOFF_NOT_FOUND));
        if (dayoff.getDayoffType() != CounselorDayoff.DayoffType.BREAKTIME) {
            throw new BusinessException(ErrorCode.DAYOFF_NOT_FOUND);
        }
        dayoffRepository.delete(dayoff);
        log.info("Breaktime deleted: dayoffId={}, counselorId={}", dayoffId, counselorId);
    }

    // ===== 휴무일 삭제 =====

    @Transactional
    public void deleteDayoff(Long counselorId, Long dayoffId) {
        CounselorDayoff dayoff = dayoffRepository.findByIdAndCounselorId(dayoffId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAYOFF_NOT_FOUND));
        if (dayoff.getDayoffType() == CounselorDayoff.DayoffType.BREAKTIME) {
            throw new BusinessException(ErrorCode.DAYOFF_NOT_FOUND);
        }
        dayoffRepository.delete(dayoff);
        log.info("Dayoff deleted: dayoffId={}, counselorId={}", dayoffId, counselorId);
    }

    // ===== 슬롯 단건 비활성화 =====

    @Transactional
    public void deactivateSchedule(Long counselorId, Long scheduleId) {
        CounselorSchedule schedule = scheduleRepository.findByIdAndCounselorId(scheduleId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (bookingRepository.existsByScheduleIdAndStatusIn(scheduleId, ACTIVE_STATUSES)) {
            throw new BusinessException(ErrorCode.SCHEDULE_CANNOT_DEACTIVATE_ACTIVE_BOOKING);
        }
        schedule.markUnavailable();
        log.info("Schedule deactivated: scheduleId={}, counselorId={}", scheduleId, counselorId);
    }

    // ===== 슬롯 단건 활성화 =====

    @Transactional
    public void activateSchedule(Long counselorId, Long scheduleId) {
        CounselorSchedule schedule = scheduleRepository.findByIdAndCounselorId(scheduleId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.markAvailable();
        log.info("Schedule activated: scheduleId={}, counselorId={}", scheduleId, counselorId);
    }

    // ===== 날짜 전체 비활성화 =====

    @Transactional
    public void closeSchedule(Long counselorId, LocalDate date) {
        long activeCount = bookingRepository.countByCounselorIdAndBookedDateAndStatusIn(
                counselorId, date, ACTIVE_STATUSES);
        if (activeCount > 0) {
            throw new BusinessException(ErrorCode.SCHEDULE_CANNOT_CLOSE_ACTIVE_BOOKING);
        }
        List<CounselorSchedule> schedules =
                scheduleRepository.findByCounselorIdAndSlotDateOrderByStartTime(counselorId, date);
        schedules.forEach(CounselorSchedule::markUnavailable);
        log.info("Schedule closed: counselorId={}, date={}, count={}", counselorId, date, schedules.size());
    }

    // ===== 일정 단건 삭제 =====

    @Transactional
    public void deleteSchedule(Long counselorId, Long scheduleId) {
        CounselorSchedule schedule = scheduleRepository.findByIdAndCounselorId(scheduleId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (bookingRepository.existsByScheduleIdAndStatusIn(scheduleId, ACTIVE_STATUSES)) {
            throw new BusinessException(ErrorCode.SCHEDULE_CANNOT_DELETE_ACTIVE_BOOKING);
        }
        scheduleRepository.delete(schedule);
        log.info("Schedule deleted: scheduleId={}, counselorId={}", scheduleId, counselorId);
    }

    // ===== 일정 일괄 삭제 (예약 없는 슬롯만) =====

    @Transactional
    public ScheduleManageDto.BulkDeleteResult bulkDeleteSchedules(Long counselorId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        Set<Long> activeScheduleIds = new HashSet<>(
                bookingRepository.findScheduleIdsWithActiveBookings(counselorId, startDate, endDate, ACTIVE_STATUSES));
        List<CounselorSchedule> schedules =
                scheduleRepository.findByCounselorIdAndSlotDateBetween(counselorId, startDate, endDate);
        List<CounselorSchedule> deletable = schedules.stream()
                .filter(s -> !activeScheduleIds.contains(s.getId()))
                .toList();
        scheduleRepository.deleteAll(deletable);
        log.info("Bulk delete: counselorId={}, deleted={}", counselorId, deletable.size());
        return ScheduleManageDto.BulkDeleteResult.builder()
                .deletedCount(deletable.size())
                .build();
    }

    // ===== 월간 캘린더 마킹 =====

    @Transactional(readOnly = true)
    public ScheduleManageDto.MonthlyScheduleResponse getMonthlyDates(Long counselorId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end   = month.atEndOfMonth();
        List<LocalDate> dates = scheduleRepository
                .findDistinctSlotDatesByCounselorAndMonth(counselorId, start, end);
        return ScheduleManageDto.MonthlyScheduleResponse.builder()
                .dates(dates)
                .build();
    }

    // ===== 일간 슬롯 조회 (상담사용) =====

    @Transactional(readOnly = true)
    public ScheduleManageDto.DailyScheduleResponse getDailySchedule(Long counselorId, LocalDate date) {
        CounselorScheduleSettings settings = settingsRepository.findByCounselorId(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_SETTINGS_NOT_FOUND));
        List<CounselorSchedule> schedules =
                scheduleRepository.findByCounselorIdAndSlotDateOrderByStartTime(counselorId, date);
        return buildDailyResponse(date, settings.getSlotUnit(), schedules);
    }

    // ===== 일간 가용 슬롯 조회 (내담자용) =====

    @Transactional(readOnly = true)
    public ScheduleManageDto.DailyScheduleResponse getAvailableSchedule(Long counselorId, LocalDate date) {
        CounselorScheduleSettings settings = settingsRepository.findByCounselorId(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_SETTINGS_NOT_FOUND));
        List<CounselorSchedule> schedules =
                scheduleRepository.findByCounselorIdAndSlotDateAndIsAvailableTrueOrderByStartTime(counselorId, date);
        return buildDailyResponse(date, settings.getSlotUnit(), schedules);
    }

    // ===== 내부 유틸 =====

    private ScheduleManageDto.DailyScheduleResponse buildDailyResponse(
            LocalDate date, int slotUnit, List<CounselorSchedule> schedules) {
        List<ScheduleManageDto.SlotResponse> morning = schedules.stream()
                .filter(s -> !s.getStartTime().isBefore(MORNING_START)
                          && s.getStartTime().isBefore(MORNING_END))
                .map(this::toSlotResponse)
                .toList();
        List<ScheduleManageDto.SlotResponse> afternoon = schedules.stream()
                .filter(s -> !s.getStartTime().isBefore(MORNING_END)
                          && s.getStartTime().isBefore(AFTERNOON_END))
                .map(this::toSlotResponse)
                .toList();
        return ScheduleManageDto.DailyScheduleResponse.builder()
                .date(date)
                .slotUnit(slotUnit)
                .morning(morning)
                .afternoon(afternoon)
                .build();
    }

    private boolean overlapsBreaktimes(LocalTime start, LocalTime end, List<CounselorDayoff> breaktimes) {
        return breaktimes.stream()
                .anyMatch(bt -> start.isBefore(bt.getEndTime()) && end.isAfter(bt.getStartTime()));
    }

    private void validateFutureDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
        }
    }

    private ScheduleManageDto.SlotResponse toSlotResponse(CounselorSchedule s) {
        return ScheduleManageDto.SlotResponse.builder()
                .scheduleId(s.getId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .isAvailable(s.isAvailable())
                .build();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private SessionType findSessionTypeOrThrow(Long sessionTypeId) {
        return sessionTypeRepository.findById(sessionTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
