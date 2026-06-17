package com.rapport.domain.booking.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorSessionTypeRepository extends JpaRepository<CounselorSessionType, Long> {
    @Query("""
            SELECT cst
            FROM CounselorSessionType cst
            JOIN FETCH cst.sessionType st
            WHERE cst.counselor.id = :counselorId
            """)
    List<CounselorSessionType> findAllByCounselorId(Long counselorId);

    @Query(
            value = """
                    SELECT cst.counselor_id, MIN(cst.price) AS min_price
                    FROM counselor_session_types cst
                    WHERE cst.counselor_id IN (:counselorIds)
                    GROUP BY cst.counselor_id
                    """,
            nativeQuery = true
    )
    List<Object[]> findMinPriceByCounselorIds(@Param("counselorIds") List<Long> counselorIds);

    @Query(
            value = """
                    SELECT cst.counselor_id, st.name
                    FROM counselor_session_types cst
                    JOIN session_types st ON st.id = cst.session_type_id
                    WHERE cst.counselor_id IN (:counselorIds)
                    """,
            nativeQuery = true
    )
    List<Object[]> findSessionTypeNamesByCounselorIds(@Param("counselorIds") List<Long> counselorIds);

    boolean existsByCounselorIdAndSessionTypeId(Long counselorId, Long sessionTypeId);
    Optional<CounselorSessionType> findByIdAndCounselorId(Long id, Long counselorId);
    Optional<CounselorSessionType> findByCounselorIdAndSessionTypeId(Long counselorId, Long sessionTypeId);
}
