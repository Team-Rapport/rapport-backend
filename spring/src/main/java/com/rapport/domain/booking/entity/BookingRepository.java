package com.rapport.domain.booking.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 내담자 예약 목록
    Page<Booking> findAllByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    // 상담사 예약 목록
    Page<Booking> findAllByCounselorIdOrderByCreatedAtDesc(Long counselorId, Pageable pageable);

    // 단건 조회 (내담자용)
    Optional<Booking> findByIdAndClientId(Long bookingId, Long clientId);

    // 단건 조회 (상담사용)
    Optional<Booking> findByIdAndCounselorId(Long bookingId, Long counselorId);

    // 대시보드: 내담자 진행 예정 예약 (오늘 이후, ACCEPTED 상태)
    @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId " +
           "AND b.status = 'ACCEPTED' AND b.bookedDate >= :today " +
           "ORDER BY b.bookedDate, b.bookedStartTime")
    List<Booking> findUpcomingByClient(Long clientId, LocalDate today);

    // 대시보드: 상담사 오늘 일정
    @Query("SELECT b FROM Booking b WHERE b.counselor.id = :counselorId " +
           "AND b.status = 'ACCEPTED' AND b.bookedDate = :today " +
           "ORDER BY b.bookedStartTime")
    List<Booking> findTodayByCounselor(Long counselorId, LocalDate today);

    // 캘린더: 날짜별 상담사 예약 (시간순)
    @Query("SELECT b FROM Booking b WHERE b.counselor.id = :counselorId " +
           "AND b.bookedDate = :date AND b.status = 'ACCEPTED' " +
           "ORDER BY b.bookedStartTime")
    List<Booking> findDailyByCounselor(Long counselorId, LocalDate date);

    // 캘린더: 월별 예약 있는 날짜 목록
    @Query("SELECT b.bookedDate FROM Booking b WHERE b.counselor.id = :counselorId " +
           "AND b.bookedDate BETWEEN :start AND :end " +
           "AND b.status = 'ACCEPTED'")
    List<LocalDate> findBookedDatesByCounselorAndMonth(Long counselorId,
                                                        LocalDate start,
                                                        LocalDate end);

    // 상담사-내담자 간 예약 이력 전체
    List<Booking> findAllByCounselorIdAndClientIdOrderByCreatedAtDesc(Long counselorId,
                                                                       Long clientId);

    // 상담사 미처리 예약 수
    long countByCounselorIdAndStatus(Long counselorId, Booking.BookingStatus status);

    // 마이페이지 통계: 내담자 완료 상담 수
    long countByClientIdAndStatus(Long clientId, Booking.BookingStatus status);
}
