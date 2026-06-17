package com.rapport.domain.booking.service;

import com.rapport.domain.booking.dto.BookingDto;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.booking.entity.CounselorSchedule;
import com.rapport.domain.booking.entity.CounselorScheduleRepository;
import com.rapport.domain.booking.entity.CounselorSessionTypeRepository;
import com.rapport.domain.booking.entity.SessionType;
import com.rapport.domain.booking.entity.SessionTypeRepository;
import com.rapport.domain.notification.service.NotificationService;
import com.rapport.domain.report.entity.Report;
import com.rapport.domain.report.entity.ReportRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository               bookingRepository;
    private final CounselorScheduleRepository     scheduleRepository;
    private final SessionTypeRepository           sessionTypeRepository;
    private final CounselorSessionTypeRepository  counselorSessionTypeRepository;
    private final ReportRepository                reportRepository;
    private final UserRepository                  userRepository;
    private final NotificationService             notificationService;

    // ===== 가용 슬롯 조회 =====
    @Transactional(readOnly = true)
    public List<BookingDto.ScheduleSlotResponse> getAvailableSlots(Long counselorId) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        return scheduleRepository.findAvailableSlots(counselorId, today)
                .stream()
                .filter(s -> s.getSlotDate().isAfter(today)
                          || s.getStartTime().isAfter(now))
                .map(s -> BookingDto.ScheduleSlotResponse.builder()
                        .scheduleId(s.getId())
                        .slotDate(s.getSlotDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .sessionTypeName(s.getSessionType().getName())
                        .build())
                .toList();
    }

    // ===== 예약 생성 (내담자) =====
    @Transactional
    public BookingDto.BookingResponse createBooking(Long clientId,
                                                     BookingDto.CreateRequest req) {
        User client   = findUserOrThrow(clientId);
        User counselor = findUserOrThrow(req.getCounselorId());

        CounselorSchedule schedule = scheduleRepository
                .findByIdAndIsAvailableTrue(req.getScheduleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_AVAILABLE));

        SessionType sessionType = sessionTypeRepository.findById(req.getSessionTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!counselorSessionTypeRepository.existsByCounselorIdAndSessionTypeId(
                req.getCounselorId(), req.getSessionTypeId())) {
            throw new BusinessException(ErrorCode.COUNSELOR_SESSION_TYPE_NOT_OFFERED);
        }

        Report report = null;
        if (req.getReportId() != null) {
            report = reportRepository.findByIdAndClientId(req.getReportId(), clientId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        // 슬롯 선점
        schedule.markUnavailable();

        // ★ concern 포함하여 예약 생성
        Booking booking = Booking.create(client, counselor, schedule,
                sessionType, report, req.getConcern());
        bookingRepository.save(booking);

        // 상담사에게 예약 요청 알림
        notificationService.notifyBookingReceived(
                counselor, booking.getId(), client.getName());

        log.info("Booking created: bookingId={}, clientId={}, counselorId={}",
                booking.getId(), clientId, req.getCounselorId());
        return toResponse(booking);
    }

    // ===== 예약 수락 (상담사) =====
    @Transactional
    public BookingDto.BookingResponse acceptBooking(Long bookingId, Long counselorId) {
        Booking booking = bookingRepository.findByIdAndCounselorId(bookingId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        booking.accept();

        notificationService.notifyBookingConfirmed(
                booking.getClient(), bookingId, booking.getCounselor().getName());

        log.info("Booking accepted: bookingId={}", bookingId);
        return toResponse(booking);
    }

    // ===== 예약 거절 (상담사) =====
    @Transactional
    public BookingDto.BookingResponse rejectBooking(Long bookingId, Long counselorId) {
        Booking booking = bookingRepository.findByIdAndCounselorId(bookingId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        booking.reject();

        notificationService.notifyBookingRejected(
                booking.getClient(), bookingId, booking.getCounselor().getName());

        log.info("Booking rejected: bookingId={}", bookingId);
        return toResponse(booking);
    }

    // ===== 예약 취소 (내담자) =====
    @Transactional
    public BookingDto.BookingResponse cancelBooking(Long bookingId, Long clientId,
                                                     String reason) {
        Booking booking = bookingRepository.findByIdAndClientId(bookingId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        booking.cancelByClient(reason);

        notificationService.notifyBookingCancelled(
                booking.getCounselor(), bookingId, booking.getClient().getName());

        log.info("Booking cancelled: bookingId={}, clientId={}", bookingId, clientId);
        return toResponse(booking);
    }

    // ===== 예약 취소 (상담사) =====
    @Transactional
    public BookingDto.BookingResponse cancelBookingByCounselor(Long bookingId, Long counselorId,
                                                                String reason) {
        Booking booking = bookingRepository.findByIdAndCounselorId(bookingId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED
                || booking.getStatus() == Booking.BookingStatus.CANCELLED
                || booking.getStatus() == Booking.BookingStatus.REJECTED) {
            throw new BusinessException(ErrorCode.BOOKING_CANCEL_NOT_ALLOWED);
        }
        booking.cancelByCounselor(reason);

        notificationService.notifyBookingCancelled(
                booking.getClient(), bookingId, booking.getCounselor().getName());

        log.info("Booking cancelled by counselor: bookingId={}, counselorId={}", bookingId, counselorId);
        return toResponse(booking);
    }

    // ===== 내담자 예약 목록 =====
    @Transactional(readOnly = true)
    public Page<BookingDto.BookingResponse> getClientBookings(Long clientId,
                                                               Pageable pageable) {
        return bookingRepository.findAllByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toResponse);
    }

    // ===== 상담사 예약 목록 =====
    @Transactional(readOnly = true)
    public Page<BookingDto.BookingResponse> getCounselorBookings(Long counselorId,
                                                                  Pageable pageable) {
        return bookingRepository.findAllByCounselorIdOrderByCreatedAtDesc(counselorId, pageable)
                .map(this::toResponse);
    }

    // ===== 내부 유틸 =====
    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public BookingDto.BookingResponse toResponse(Booking b) {
        return BookingDto.BookingResponse.builder()
                .bookingId(b.getId())
                .status(b.getStatus())
                .counselorId(b.getCounselor().getId())
                .counselorName(b.getCounselor().getName())
                .clientId(b.getClient().getId())
                .clientName(b.getClient().getName())
                .bookedDate(b.getBookedDate())
                .bookedStartTime(b.getBookedStartTime())
                .bookedEndTime(b.getBookedEndTime())
                .sessionTypeName(b.getSessionType().getName())
                .concern(b.getConcern())         // ★ V2 추가
                .cancellationReason(b.getCancellationReason())
                .cancelledBy(b.getCancelledBy())
                .cancelledAt(b.getCancelledAt())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
