package com.rapport.domain.counselor.service;

import com.rapport.domain.counselor.dto.CounselorDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselorApprovalService {

    private final CounselorProfileRepository counselorProfileRepository;
    private final EmailService emailService;

    // ===== 심사 대기 목록 조회 =====

    @Transactional(readOnly = true)
    public Page<CounselorDto.PendingCounselorResponse> getPendingCounselors(Pageable pageable) {
        return counselorProfileRepository
                .findAllByApprovalStatus(CounselorProfile.ApprovalStatus.PENDING, pageable)
                .map(this::toPendingResponse);
    }

    // ===== 승인 처리 =====

    @Transactional
    public CounselorDto.ApprovalResultResponse approve(Long counselorUserId) {
        CounselorProfile profile = findProfileOrThrow(counselorUserId);

        if (!profile.isPending()) {
            throw new BusinessException(ErrorCode.COUNSELOR_APPROVAL_ALREADY_PROCESSED);
        }

        profile.approve(counselorUserId);
        User counselor = profile.getUser();

        // 비동기 이메일 발송
        emailService.sendCounselorApprovalEmail(counselor.getEmail(), counselor.getName());

        log.info("Counselor approved: userId={}, name={}", counselorUserId, counselor.getName());

        return CounselorDto.ApprovalResultResponse.builder()
                .userId(counselorUserId)
                .name(counselor.getName())
                .status(CounselorProfile.ApprovalStatus.APPROVED)
                .processedAt(LocalDateTime.now())
                .message("승인이 완료되었습니다. 상담사에게 이메일이 발송되었습니다.")
                .build();
    }

    // ===== 반려 처리 =====

    @Transactional
    public CounselorDto.ApprovalResultResponse reject(Long counselorUserId, String reason) {
        CounselorProfile profile = findProfileOrThrow(counselorUserId);

        if (!profile.isPending()) {
            throw new BusinessException(ErrorCode.COUNSELOR_APPROVAL_ALREADY_PROCESSED);
        }

        profile.reject(reason);
        User counselor = profile.getUser();

        // 비동기 이메일 발송
        emailService.sendCounselorRejectionEmail(
                counselor.getEmail(), counselor.getName(), reason);

        log.info("Counselor rejected: userId={}, reason={}", counselorUserId, reason);

        return CounselorDto.ApprovalResultResponse.builder()
                .userId(counselorUserId)
                .name(counselor.getName())
                .status(CounselorProfile.ApprovalStatus.REJECTED)
                .processedAt(LocalDateTime.now())
                .message("반려 처리가 완료되었습니다. 상담사에게 사유가 포함된 이메일이 발송되었습니다.")
                .build();
    }

    // ===== 내부 유틸 =====

    private CounselorProfile findProfileOrThrow(Long userId) {
        return counselorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));
    }

    private CounselorDto.PendingCounselorResponse toPendingResponse(CounselorProfile profile) {
        return CounselorDto.PendingCounselorResponse.builder()
                .userId(profile.getUser().getId())
                .profileId(profile.getId())
                .name(profile.getUser().getName())
                .email(profile.getUser().getEmail())
                .licenseType(profile.getLicenseType())
                .licenseNumber(profile.getLicenseNumber())
                .approvalStatus(profile.getApprovalStatus())
                .appliedAt(profile.getCreatedAt())
                .build();
    }

    @Transactional
    public void reapply(Long counselorUserId) {
        CounselorProfile profile = findProfileOrThrow(counselorUserId);
        profile.reapply();
        log.info("Counselor reapplied: userId={}", counselorUserId);
    }
}
