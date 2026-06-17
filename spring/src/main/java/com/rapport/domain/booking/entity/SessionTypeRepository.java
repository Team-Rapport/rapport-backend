package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SessionTypeRepository extends JpaRepository<SessionType, Long> {
    Optional<SessionType> findByName(String name);
}
