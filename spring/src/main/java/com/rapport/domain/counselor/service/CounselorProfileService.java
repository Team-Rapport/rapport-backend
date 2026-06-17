package com.rapport.domain.counselor.service;

import com.rapport.domain.booking.entity.CounselorSessionTypeRepository;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselorProfileService {

    private final CounselorProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CounselorSessionTypeRepository counselorSessionTypeRepository;

    // ===== 상담사 본인 프로필 조회 =====
    @Transactional(readOnly = true)
    public CounselorProfileDto.MyProfileResponse getMyProfile(Long userId) {
        CounselorProfile profile = findByUserIdOrThrow(userId);
        User user = profile.getUser();
        List<String> missingFields = getRequiredMissingFields(profile);
        List<CounselorProfileDto.ConsultationMode> consultationModes = resolveConsultationModes(
                profile.getConsultationModes(),
                profile.getApproaches(),
                getConsultationModeMap(List.of(userId)).getOrDefault(userId, List.of())
        );
        return CounselorProfileDto.MyProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .licenseType(profile.getLicenseType())
                .licenseNumber(profile.getLicenseNumber())
                .counselorGender(profile.getCounselorGender())
                .specializations(defaultList(profile.getSpecializations()))
                .approaches(filterApproaches(defaultList(profile.getApproaches())))
                .symptoms(defaultList(profile.getSymptoms()))
                .consultationModes(consultationModes)
                .bio(profile.getBio())
                .experienceYears(profile.getExperienceYears())
                .officeAddress(profile.getOfficeAddress())
                .averageRating(profile.getAverageRating())
                .reviewCount(profile.getReviewCount())
                .approvalStatus(profile.getApprovalStatus())
                .rejectionReason(profile.getRejectionReason())
                .approvedAt(profile.getApprovedAt())
                .profileCompleted(missingFields.isEmpty())
                .requiredMissingFields(missingFields)
                .build();
    }

    // ===== 상담사 프로필 수정 =====
    @Transactional
    public CounselorProfileDto.MyProfileResponse updateMyProfile(Long userId,
            CounselorProfileDto.CounselorProfileUpdateRequest request) {
        CounselorProfile profile = findByUserIdOrThrow(userId);
        if (request.getLicenseType() != null || request.getLicenseNumber() != null) {
            log.info("Ignored license fields in counselor profile patch: userId={}", userId);
        }
        profile.update(
                null,
                null,
                request.getCounselorGender(),
                request.getSpecializations(),
                filterApproaches(defaultList(request.getApproaches())),
                request.getSymptoms(),
                toConsultationModeStrings(request.getConsultationModes()),
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
        Map<Long, Integer> minPriceMap = getMinPriceMap(List.of(userId));
        Map<Long, List<CounselorProfileDto.ConsultationMode>> modeMap = getConsultationModeMap(List.of(userId));
        return toPublicResponse(profile, minPriceMap, modeMap);
    }

    // ===== 승인된 상담사 목록 조회 (내담자용) =====
    @Transactional(readOnly = true)
    public Page<CounselorProfileDto.PublicProfileResponse> getApprovedCounselors(Pageable pageable) {
        Page<CounselorProfile> page = profileRepository
                .findAllByApprovalStatus(CounselorProfile.ApprovalStatus.APPROVED, pageable);
        List<Long> counselorIds = page.getContent().stream().map(p -> p.getUser().getId()).toList();
        Map<Long, Integer> minPriceMap = getMinPriceMap(counselorIds);
        Map<Long, List<CounselorProfileDto.ConsultationMode>> modeMap = getConsultationModeMap(counselorIds);
        return page.map(p -> toPublicResponse(p, minPriceMap, modeMap));
    }

    // ===== 내부 유틸 =====
    private CounselorProfile findByUserIdOrThrow(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));
    }

    private CounselorProfileDto.PublicProfileResponse toPublicResponse(
            CounselorProfile profile,
            Map<Long, Integer> minPriceMap,
            Map<Long, List<CounselorProfileDto.ConsultationMode>> modeMap
    ) {
        Long counselorId = profile.getUser().getId();
        return CounselorProfileDto.PublicProfileResponse.builder()
                .userId(counselorId)
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .licenseType(profile.getLicenseType())
                .counselorGender(profile.getCounselorGender())
                .specializations(defaultList(profile.getSpecializations()))
                .approaches(filterApproaches(defaultList(profile.getApproaches())))
                .symptoms(defaultList(profile.getSymptoms()))
                .consultationModes(resolveConsultationModes(
                        profile.getConsultationModes(),
                        profile.getApproaches(),
                        modeMap.getOrDefault(counselorId, List.of())
                ))
                .minPrice(minPriceMap.get(counselorId))
                .bio(profile.getBio())
                .experienceYears(profile.getExperienceYears())
                .averageRating(profile.getAverageRating())
                .reviewCount(profile.getReviewCount())
                .approvalStatus(profile.getApprovalStatus())
                .approvedAt(profile.getApprovedAt())
                .build();
    }

    private Map<Long, Integer> getMinPriceMap(List<Long> counselorIds) {
        if (counselorIds == null || counselorIds.isEmpty()) return Map.of();
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : counselorSessionTypeRepository.findMinPriceByCounselorIds(counselorIds)) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return result;
    }

    private Map<Long, List<CounselorProfileDto.ConsultationMode>> getConsultationModeMap(List<Long> counselorIds) {
        if (counselorIds == null || counselorIds.isEmpty()) return Map.of();
        Map<Long, List<CounselorProfileDto.ConsultationMode>> result = new HashMap<>();
        for (Object[] row : counselorSessionTypeRepository.findSessionTypeNamesByCounselorIds(counselorIds)) {
            Long counselorId = ((Number) row[0]).longValue();
            String sessionType = String.valueOf(row[1]);
            CounselorProfileDto.ConsultationMode mode = toConsultationMode(sessionType);
            if (mode == null) continue;
            result.computeIfAbsent(counselorId, k -> new ArrayList<>());
            if (!result.get(counselorId).contains(mode)) {
                result.get(counselorId).add(mode);
            }
        }
        return result;
    }

    private CounselorProfileDto.ConsultationMode toConsultationMode(String sessionType) {
        if (sessionType == null) return null;
        return switch (sessionType) {
            case "MEETING" -> CounselorProfileDto.ConsultationMode.FACE_TO_FACE;
            case "CHAT", "CALL", "VIDEOCALL" -> CounselorProfileDto.ConsultationMode.ONLINE;
            default -> null;
        };
    }

    private List<String> getRequiredMissingFields(CounselorProfile profile) {
        List<String> missing = new ArrayList<>();
        if (profile.getCounselorGender() == null) missing.add("counselorGender");
        if (profile.getBio() == null || profile.getBio().isBlank()) missing.add("bio");
        if (profile.getSpecializations() == null || profile.getSpecializations().isEmpty()) missing.add("specializations");
        if (profile.getSymptoms() == null || profile.getSymptoms().isEmpty()) missing.add("symptoms");
        if (resolveConsultationModes(profile.getConsultationModes(), profile.getApproaches(), List.of()).isEmpty()) {
            missing.add("consultationModes");
        }
        if (filterApproaches(defaultList(profile.getApproaches())).isEmpty()) missing.add("approaches");
        if (profile.getExperienceYears() == null) missing.add("experienceYears");
        if (profile.getOfficeAddress() == null || profile.getOfficeAddress().isBlank()) missing.add("officeAddress");
        return missing;
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<String> filterApproaches(List<String> approaches) {
        if (approaches == null || approaches.isEmpty()) return List.of();
        return approaches.stream()
                .filter(v -> !isLegacyConsultationValue(v))
                .toList();
    }

    private boolean isLegacyConsultationValue(String value) {
        if (value == null) return false;
        return switch (value.trim().toUpperCase()) {
            case "MEETING", "CALL", "CHAT", "VIDEOCALL", "FACE_TO_FACE", "ONLINE" -> true;
            default -> false;
        };
    }

    private List<String> toConsultationModeStrings(List<CounselorProfileDto.ConsultationMode> modes) {
        if (modes == null) return null;
        return modes.stream().map(Enum::name).toList();
    }

    private List<CounselorProfileDto.ConsultationMode> resolveConsultationModes(
            List<String> profileModes,
            List<String> approaches,
            List<CounselorProfileDto.ConsultationMode> fallbackFromSessionTypes
    ) {
        EnumSet<CounselorProfileDto.ConsultationMode> result = EnumSet.noneOf(CounselorProfileDto.ConsultationMode.class);

        if (profileModes != null) {
            for (String mode : profileModes) {
                try {
                    result.add(CounselorProfileDto.ConsultationMode.valueOf(mode));
                } catch (Exception ignored) {
                }
            }
        }

        if (approaches != null) {
            for (String value : approaches) {
                CounselorProfileDto.ConsultationMode mapped = mapLegacyConsultationMode(value);
                if (mapped != null) result.add(mapped);
            }
        }

        if (result.isEmpty() && fallbackFromSessionTypes != null) {
            result.addAll(fallbackFromSessionTypes);
        }

        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    private CounselorProfileDto.ConsultationMode mapLegacyConsultationMode(String value) {
        if (value == null) return null;
        return switch (value.trim().toUpperCase()) {
            case "MEETING", "FACE_TO_FACE" -> CounselorProfileDto.ConsultationMode.FACE_TO_FACE;
            case "CALL", "CHAT", "VIDEOCALL", "ONLINE" -> CounselorProfileDto.ConsultationMode.ONLINE;
            default -> null;
        };
    }
}
