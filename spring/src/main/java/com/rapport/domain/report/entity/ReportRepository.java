package com.rapport.domain.report.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findAllByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);
    Optional<Report> findByIdAndClientId(Long reportId, Long clientId);
    boolean existsBySessionId(Long sessionId);
    Optional<Report> findBySessionId(Long sessionId);
}
