package com.rapport.domain.counselor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapport.domain.booking.entity.CounselorSessionTypeRepository;
import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CounselorSearchService {

    @PersistenceContext
    private EntityManager em;
    private final CounselorSessionTypeRepository counselorSessionTypeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CounselorProfileDto.PublicProfileResponse> search(
            List<String> specializations,
            List<String> sessionTypes,
            List<String> genders,
            List<String> approaches,
            int page,
            int size) {

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT
                    cp.id, cp.user_id, cp.license_type, cp.counselor_gender,
                    cp.specializations, cp.approaches, cp.symptoms, cp.consultation_modes, cp.bio, cp.experience_years,
                    cp.average_rating, cp.review_count, cp.approval_status, cp.approved_at,
                    u.name, u.email, u.profile_image_url,
                    cp.created_at
                FROM counselor_profiles cp
                JOIN users u ON u.id = cp.user_id AND u.deleted_at IS NULL
                WHERE cp.approval_status = 'APPROVED'
                AND cp.deleted_at IS NULL
                """);

        List<Object> params = new ArrayList<>();

        // 성별 필터
        if (genders != null && !genders.isEmpty()) {
            sql.append("AND cp.counselor_gender IN (");
            for (int i = 0; i < genders.size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
                params.add(genders.get(i));
            }
            sql.append(") ");
        }

        // 전문분야 필터 (OR)
        if (specializations != null && !specializations.isEmpty()) {
            sql.append("AND (");
            for (int i = 0; i < specializations.size(); i++) {
                if (i > 0) sql.append("OR ");
                sql.append("JSON_CONTAINS(cp.specializations, ?, '$') = 1 ");
                params.add("\"" + specializations.get(i) + "\"");
            }
            sql.append(") ");
        }

        // 접근법 필터 (OR)
        if (approaches != null && !approaches.isEmpty()) {
            sql.append("AND (");
            for (int i = 0; i < approaches.size(); i++) {
                if (i > 0) sql.append("OR ");
                sql.append("JSON_CONTAINS(cp.approaches, ?, '$') = 1 ");
                params.add("\"" + approaches.get(i) + "\"");
            }
            sql.append(") ");
        }

        // 상담 방식 필터
        if (sessionTypes != null && !sessionTypes.isEmpty()) {
            sql.append("AND EXISTS (SELECT 1 FROM counselor_session_types cst ");
            sql.append("JOIN session_types st ON st.id = cst.session_type_id ");
            sql.append("WHERE cst.counselor_id = cp.user_id AND st.name IN (");
            for (int i = 0; i < sessionTypes.size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
                params.add(sessionTypes.get(i));
            }
            sql.append(")) ");
        }

        sql.append("ORDER BY cp.created_at DESC ");
        sql.append("LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        var query = em.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<Long> counselorIds = rows.stream()
                .map(row -> ((Number) row[1]).longValue())
                .toList();
        Map<Long, Integer> minPriceMap = getMinPriceMap(counselorIds);
        Map<Long, List<CounselorProfileDto.ConsultationMode>> modeMap = getConsultationModeMap(counselorIds);

        return rows.stream().map(row -> {
            Long counselorId = ((Number) row[1]).longValue();
            return CounselorProfileDto.PublicProfileResponse.builder()
                    .userId(counselorId)
                    .licenseType((String) row[2])
                    .counselorGender(CounselorProfile.CounselorGender.valueOf((String) row[3]))
                    .specializations(parseJsonArray(row[4]))
                    .approaches(filterApproaches(parseJsonArray(row[5])))
                    .symptoms(parseJsonArray(row[6]))
                    .consultationModes(resolveConsultationModes(
                            parseJsonArray(row[7]),
                            parseJsonArray(row[5]),
                            modeMap.getOrDefault(counselorId, List.of())
                    ))
                    .minPrice(minPriceMap.get(counselorId))
                    .bio((String) row[8])
                    .experienceYears(row[9] != null ? ((Number) row[9]).intValue() : null)
                    .averageRating(row[10] != null ? new BigDecimal(row[10].toString()) : null)
                    .reviewCount(row[11] != null ? ((Number) row[11]).intValue() : 0)
                    .approvalStatus(CounselorProfile.ApprovalStatus.valueOf((String) row[12]))
                    .name((String) row[14])
                    .profileImageUrl((String) row[16])
                    .build();
        }).toList();
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
            CounselorProfileDto.ConsultationMode mode = toConsultationMode(String.valueOf(row[1]));
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

    private List<String> parseJsonArray(Object value) {
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
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
