package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounselorSessionTypeRepository extends JpaRepository<CounselorSessionType, Long> {
    List<CounselorSessionType> findAllByCounselorId(Long counselorId);
    boolean existsByCounselorIdAndSessionTypeId(Long counselorId, Long sessionTypeId);
}
