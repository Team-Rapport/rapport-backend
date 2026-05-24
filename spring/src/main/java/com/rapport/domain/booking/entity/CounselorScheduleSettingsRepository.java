package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CounselorScheduleSettingsRepository extends JpaRepository<CounselorScheduleSettings, Long> {
    Optional<CounselorScheduleSettings> findByCounselorId(Long counselorId);
    boolean existsByCounselorId(Long counselorId);
}
