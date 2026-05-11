package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorScheduleRepository extends JpaRepository<CounselorSchedule, Long> {

    @Query("SELECT s FROM CounselorSchedule s " +
           "WHERE s.counselor.id = :counselorId " +
           "AND s.slotDate >= :from AND s.isAvailable = true " +
           "ORDER BY s.slotDate, s.startTime")
    List<CounselorSchedule> findAvailableSlots(Long counselorId, LocalDate from);

    Optional<CounselorSchedule> findByIdAndIsAvailableTrue(Long id);

    // ===== 슬롯 관리 =====

    Optional<CounselorSchedule> findByIdAndCounselorId(Long id, Long counselorId);

    List<CounselorSchedule> findByCounselorIdAndSlotDateOrderByStartTime(Long counselorId, LocalDate slotDate);

    List<CounselorSchedule> findByCounselorIdAndSlotDateAndIsAvailableTrueOrderByStartTime(
            Long counselorId, LocalDate slotDate);

    List<CounselorSchedule> findByCounselorIdAndSlotDateBetween(
            Long counselorId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT s.slotDate FROM CounselorSchedule s " +
           "WHERE s.counselor.id = :counselorId " +
           "AND s.slotDate BETWEEN :start AND :end " +
           "ORDER BY s.slotDate")
    List<LocalDate> findDistinctSlotDatesByCounselorAndMonth(@Param("counselorId") Long counselorId,
                                                              @Param("start") LocalDate start,
                                                              @Param("end") LocalDate end);
}
