package com.rapport.domain.counselor.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorProfileRepository extends JpaRepository<CounselorProfile, Long> {

    Optional<CounselorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<CounselorProfile> findAllByApprovalStatus(CounselorProfile.ApprovalStatus status, Pageable pageable);

    @Query("SELECT cp FROM CounselorProfile cp WHERE cp.approvalStatus = 'PENDING' ORDER BY cp.createdAt ASC")
    List<CounselorProfile> findAllPendingOrderByCreatedAt();
}
