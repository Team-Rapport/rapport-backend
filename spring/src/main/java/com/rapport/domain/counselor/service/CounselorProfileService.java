package com.rapport.domain.counselor.service;

import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselorProfileService {

    private final CounselorProfileRepository profileRepository;
    private final UserRepository userRepository;

    // ===== 상담사 본인 프로필 조회 =====
    @Transactional(readOnly = true)
    public CounselorProfileDto.MyProfileResponse getMyProfile(Long userId) {
        CounselorProfile profile = findByUserIdOrThrow(userId);
        User user = profile.getUser();
        return CounselorProfileDto.MyProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .licenseType(profile.getLicenseType())
                .licenseNumber(profile.getLicenseNumber())
                .counselorGender(profile.getCounselorGender())
                .specializations(profile.getSpecializations())
                .approaches(profile.getApproaches())
                .bio(profile.getBio())
                .experienceYears(profile.getExperienceYears())
                .officeAddress(profile.getOfficeAddress())
                .averageRating(profile.getAverageRating())
                .reviewCount(profile.getReviewCount())
                .approvalStatus(profile.getApprovalStatus())
                .rejectionReason(profile.getRejectionReason())
                .approvedAt(profile.getApprovedAt())
                .build();
    }

    // ===== 상담사 프로필 수정 =====
    @Transactional
    public CounselorProfileDto.MyProfileResponse updateMyProfile(Long userId,
            CounselorProfileDto.UpdateRequest request) {
        CounselorProfile profile = findByUserIdOrThrow(userId);
        profile.update(
                request.getLicenseType(),
                request.getLicenseNumber(),
                request.getCounselorGender(),
                request.getSpecializations(),
                request.getApproaches(),
                request.getBio(),
                request.getExperienceYears(),
                request.getOfficeAddress()
        );
        log.info("Counselor profile updated: userId={}", userId);
        return getMyProfile(userId);
    }

    // ===== 공개 프로필 단건 조회 (내담자용) =====
    @Transactional(readOnly = true)
    public CounselorProfileDto.PublicProfileResponse getPublicProfile(Long userId) {
        CounselorProfile profile = profileRepository.findByUserId(userId)
                .filter(CounselorProfile::isApproved)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));
        return toPublicResponse(profile);
    }

    // ===== 승인된 상담사 목록 조회 (내담자용) =====
    @Transactional(readOnly = true)
    public Page<CounselorProfileDto.PublicProfileResponse> getApprovedCounselors(Pageable pageable) {
        return profileRepository
                .findAllByApprovalStatus(CounselorProfile.ApprovalStatus.APPROVED, pageable)
                .map(this::toPublicResponse);
    }

    // ===== 내부 유틸 =====
    private CounselorProfile findByUserIdOrThrow(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));
    }

    private CounselorProfileDto.PublicProfileResponse toPublicResponse(CounselorProfile profile) {
        return CounselorProfileDto.PublicProfileResponse.builder()
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .licenseType(profile.getLicenseType())
                .counselorGender(profile.getCounselorGender())
                .specializations(profile.getSpecializations())
                .approaches(profile.getApproaches())
                .bio(profile.getBio())
                .experienceYears(profile.getExperienceYears())
                .averageRating(profile.getAverageRating())
                .reviewCount(profile.getReviewCount())
                .approvalStatus(profile.getApprovalStatus())
                .approvedAt(profile.getApprovedAt())
                .build();
    }
}
