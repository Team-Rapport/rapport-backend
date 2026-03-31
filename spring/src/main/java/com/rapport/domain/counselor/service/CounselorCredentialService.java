package com.rapport.domain.counselor.service;

import com.rapport.domain.counselor.entity.CounselorCredential;
import com.rapport.domain.counselor.entity.CounselorCredentialRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.util.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselorCredentialService {

    private final CounselorCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    // ===== 상담사: 서류 업로드 =====

    @Transactional
    public CounselorCredential uploadCredential(Long counselorId,
                                                 CounselorCredential.CredentialType type,
                                                 MultipartFile file) {
        User counselor = userRepository.findById(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // S3에 업로드 (folder: credentials/{userId}/)
        String fileUrl = s3Service.upload(file, "credentials/" + counselorId);

        CounselorCredential credential = CounselorCredential.create(counselor, type, fileUrl);
        credentialRepository.save(credential);

        log.info("Credential uploaded: counselorId={}, type={}, url={}", counselorId, type, fileUrl);
        return credential;
    }

    // ===== 상담사: 본인 제출 서류 목록 =====

    @Transactional(readOnly = true)
    public List<CounselorCredential> getMyCredentials(Long counselorId) {
        return credentialRepository.findAllByCounselorId(counselorId);
    }

    // ===== 관리자: 특정 상담사 서류 목록 + Presigned URL 조회 =====

    @Transactional(readOnly = true)
    public List<CredentialWithUrlDto> getCredentialsForAdmin(Long counselorId) {
        List<CounselorCredential> credentials =
                credentialRepository.findAllByCounselorId(counselorId);

        // 각 파일에 대해 10분짜리 임시 열람 URL 발급
        return credentials.stream()
                .map(c -> new CredentialWithUrlDto(
                        c.getId(),
                        c.getCredentialType(),
                        c.getStatus(),
                        s3Service.generatePresignedUrl(c.getFileUrl(), 10), // 10분 유효
                        c.getCreatedAt()
                ))
                .toList();
    }

    // ===== 응답 DTO =====

    public record CredentialWithUrlDto(
            Long id,
            CounselorCredential.CredentialType type,
            CounselorCredential.CredentialStatus status,
            String viewUrl,   // 10분짜리 임시 열람 URL
            java.time.LocalDateTime uploadedAt
    ) {}
}
