package com.rapport.domain.counselor.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorProfileRepository
        extends JpaRepository<CounselorProfile, Long>,
                JpaSpecificationExecutor<CounselorProfile> {

    Optional<CounselorProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Page<CounselorProfile> findAllByApprovalStatus(
            CounselorProfile.ApprovalStatus status, Pageable pageable);

    @Query("SELECT cp FROM CounselorProfile cp " +
           "WHERE cp.approvalStatus = 'PENDING' ORDER BY cp.createdAt ASC")
    List<CounselorProfile> findAllPendingOrderByCreatedAt();

    // 관리자: 상태별 필터 (null이면 전체 조회)
    @Query("SELECT cp FROM CounselorProfile cp " +
           "WHERE (:status IS NULL OR cp.approvalStatus = :status)")
    Page<CounselorProfile> findAllByOptionalStatus(
            @Param("status") CounselorProfile.ApprovalStatus status, Pageable pageable);

    long countByApprovalStatus(CounselorProfile.ApprovalStatus status);

    // 배치: 모든 상담사 평점 초기화 (삭제되지 않은 프로필 대상)
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE CounselorProfile cp SET cp.averageRating = NULL, cp.reviewCount = 0")
    void resetAllRatings();
}
