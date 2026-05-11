package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CounselorDayoffRepository extends JpaRepository<CounselorDayoff, Long> {

    List<CounselorDayoff> findByCounselorIdAndDayoffType(Long counselorId, CounselorDayoff.DayoffType dayoffType);

    // dayoffDate IS NULL → 정기 브레이크타임
    List<CounselorDayoff> findByCounselorIdAndDayoffTypeAndDayoffDateIsNull(
            Long counselorId, CounselorDayoff.DayoffType dayoffType);

    // 특정 날짜 임시 브레이크타임
    List<CounselorDayoff> findByCounselorIdAndDayoffTypeAndDayoffDate(
            Long counselorId, CounselorDayoff.DayoffType dayoffType, LocalDate dayoffDate);

    boolean existsByCounselorIdAndDayoffTypeAndDayoffDate(
            Long counselorId, CounselorDayoff.DayoffType dayoffType, LocalDate dayoffDate);
}
