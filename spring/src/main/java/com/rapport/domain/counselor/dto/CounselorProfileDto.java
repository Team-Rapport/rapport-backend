package com.rapport.domain.counselor.dto;

import com.rapport.domain.counselor.entity.CounselorProfile;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CounselorProfileDto {

    // 프로필 수정 요청 (상담사)
    @Getter
    public static class UpdateRequest {
        @Size(max = 100)
        private String licenseType;
        private String licenseNumber;
        private CounselorProfile.CounselorGender counselorGender;
        private List<String> specializations;
        private List<String> approaches;
        @Size(max = 2000)
        private String bio;
        @Min(0) @Max(50)
        private Integer experienceYears;
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
        private List<String> approaches;
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
    }
}
