package com.rapport.domain.review.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 상담사 리뷰 목록 (삭제된 리뷰 제외 — @SQLRestriction 자동 적용)
    Page<Review> findByCounselorIdOrderByCreatedAtDesc(Long counselorId, Pageable pageable);

    // 본인 리뷰 단건 조회
    Optional<Review> findByIdAndClientId(Long id, Long clientId);

    // 예약당 1개 제한: 삭제된 리뷰 포함해서 확인 (@SQLRestriction 우회 — native query)
    @Query(value = "SELECT COUNT(*) FROM reviews WHERE booking_id = :bookingId", nativeQuery = true)
    long countByBookingIdIncludingDeleted(@Param("bookingId") Long bookingId);

    // 배치: 삭제되지 않은 리뷰 기준 상담사별 평균 평점·리뷰 수
    @Query("SELECT r.counselor.id, AVG(r.rating), COUNT(r) FROM Review r GROUP BY r.counselor.id")
    List<Object[]> findRatingStatsByCounselor();
}
