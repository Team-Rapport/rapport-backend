package com.rapport.domain.intakeform.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntakeFormRepository extends JpaRepository<IntakeForm, Long> {

    Optional<IntakeForm> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
