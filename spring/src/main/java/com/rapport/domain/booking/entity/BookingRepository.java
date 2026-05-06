package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
    List<Booking> findAllByCounselorIdOrderByCreatedAtDesc(Long counselorId);
    Optional<Booking> findByIdAndClientId(Long id, Long clientId);
    Optional<Booking> findByIdAndCounselorId(Long id, Long counselorId);
}
