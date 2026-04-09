package com.rapport.domain.counselor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CounselorCredentialRepository extends JpaRepository<CounselorCredential, Long> {
    List<CounselorCredential> findAllByCounselorId(Long counselorId);
}
