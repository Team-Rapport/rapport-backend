package com.rapport.domain.counselor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CounselorCredentialRepository extends JpaRepository<CounselorCredential, Long> {
    List<CounselorCredential> findAllByCounselorId(Long counselorId);
    boolean existsByCounselorId(Long counselorId);
    long countByCounselorId(Long counselorId);

    @Query("SELECT DISTINCT c.credentialType FROM CounselorCredential c WHERE c.counselor.id = :counselorId")
    List<CounselorCredential.CredentialType> findDistinctTypesByCounselorId(@Param("counselorId") Long counselorId);
}
