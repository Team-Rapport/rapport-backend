package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
