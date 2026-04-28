package com.rapport.domain.counselor.service;

import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CounselorSearchService {

    @PersistenceContext
    private EntityManager em;

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
                    cp.specializations, cp.approaches, cp.bio, cp.experience_years,
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

        return rows.stream().map(row -> CounselorProfileDto.PublicProfileResponse.builder()
                .userId(((Number) row[1]).longValue())
                .licenseType((String) row[2])
                .counselorGender(CounselorProfile.CounselorGender.valueOf((String) row[3]))
                .bio((String) row[6])
                .experienceYears(row[7] != null ? ((Number) row[7]).intValue() : null)
                .averageRating(row[8] != null ? new BigDecimal(row[8].toString()) : null)
                .reviewCount(row[9] != null ? ((Number) row[9]).intValue() : 0)
                .approvalStatus(CounselorProfile.ApprovalStatus.valueOf((String) row[10]))
                .name((String) row[12])
                .profileImageUrl((String) row[14])
                .build()
        ).toList();
    }
}