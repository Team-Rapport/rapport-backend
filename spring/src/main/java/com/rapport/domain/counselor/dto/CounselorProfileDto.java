package com.rapport.domain.counselor.dto;

import com.rapport.domain.counselor.entity.CounselorProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CounselorProfileDto {

    public enum ConsultationMode {
        FACE_TO_FACE, ONLINE
    }

    // 프로필 수정 요청 (상담사 전용 PATCH /api/v1/counselor/profile)
    @Getter
    @Schema(name = "CounselorProfileUpdateRequest", description = "상담사 프로필 부분 수정 요청. 미전송/NULL 필드는 기존 값 유지")
    public static class CounselorProfileUpdateRequest {
        @Schema(description = "자격 종류 (선택)", example = "상담심리사 1급")
        @Size(max = 100, message = "licenseType은 100자 이하여야 합니다.")
        private String licenseType;
        @Schema(description = "자격 번호 (선택)", example = "KCP-1-2026-1001")
        @Size(max = 100, message = "licenseNumber는 100자 이하여야 합니다.")
        private String licenseNumber;
        @Schema(description = "상담사 성별", allowableValues = {"MALE", "FEMALE", "ANY"})
        private CounselorProfile.CounselorGender counselorGender;
        @Schema(description = "전문 분야 목록", example = "[\"불안\",\"수면\",\"공황\"]")
        private List<String> specializations;
        @Schema(description = "상담 기법 목록", example = "[\"인지행동치료(CBT)\",\"호흡/이완 훈련\"]")
        private List<String> approaches;
        @Schema(description = "상담사 소개", example = "수면 문제와 불안 완화를 중심으로 일상 회복을 돕습니다.")
        @Size(max = 2000, message = "bio는 2000자 이하여야 합니다.")
        private String bio;
        @Schema(description = "경력(년)", minimum = "0", maximum = "50", example = "7")
        @Min(value = 0, message = "experienceYears는 0 이상이어야 합니다.")
        @Max(value = 50, message = "experienceYears는 50 이하여야 합니다.")
        private Integer experienceYears;
        @Schema(description = "상담실 주소", example = "서울 강남구 테헤란로 101")
        @Size(max = 500, message = "officeAddress는 500자 이하여야 합니다.")
        private String officeAddress;
    }

    // 프로필 응답 (공개용 — 내담자가 보는 상담사 프로필)
    @Getter
    @Builder
    public static class PublicProfileResponse {
        private Long userId;
        private String name;
        private String profileImageUrl;
        private String licenseType;
        private CounselorProfile.CounselorGender counselorGender;
        private List<String> specializations;
        @Schema(description = "상담 기법 목록 (전문 분야와 별도)")
        private List<String> approaches;
        @Schema(description = "상담 가능 방식 표준값: FACE_TO_FACE(대면), ONLINE(비대면)")
        private List<ConsultationMode> consultationModes;
        @Schema(description = "최소 상담 가격 (KRW, 1회 기준)")
        private Integer minPrice;
        private String bio;
        private Integer experienceYears;
        private BigDecimal averageRating;
        private int reviewCount;
        private CounselorProfile.ApprovalStatus approvalStatus;
        private LocalDateTime approvedAt;
    }

    // 프로필 응답 (본인용 — 상담사 대시보드)
    @Getter
    @Builder
    public static class MyProfileResponse {
        private Long userId;
        private String name;
        private String email;
        private String profileImageUrl;
        private String licenseType;
        private String licenseNumber;
        private CounselorProfile.CounselorGender counselorGender;
        private List<String> specializations;
        private List<String> approaches;
        private String bio;
        private Integer experienceYears;
        private String officeAddress;
        private BigDecimal averageRating;
        private int reviewCount;
        private CounselorProfile.ApprovalStatus approvalStatus;
        private String rejectionReason;
        private LocalDateTime approvedAt;
        @Schema(description = "상담사 프로필 완성 여부 (온보딩 라우팅용)")
        private boolean profileCompleted;
        @Schema(description = "완성되지 않은 필수 필드 목록", example = "[\"bio\",\"specializations\"]")
        private List<String> requiredMissingFields;
    }
}
