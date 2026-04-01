package com.rapport.domain.counselor.dto;

import com.rapport.domain.counselor.entity.CounselorProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class CounselorDto {

    // ===== 관리자: 반려 요청 =====
    @Getter
    public static class RejectRequest {
        @NotBlank(message = "반려 사유를 입력해주세요.")
        @Size(max = 1000, message = "반려 사유는 1000자 이내로 입력해주세요.")
        private String reason;
    }

    // ===== 상담사 심사 목록 응답 =====
    @Getter
    @Builder
    public static class PendingCounselorResponse {
        private Long userId;
        private Long profileId;
        private String name;
        private String email;
        private String licenseType;
        private String licenseNumber;
        private CounselorProfile.ApprovalStatus approvalStatus;
        private LocalDateTime appliedAt;
    }

    // ===== 심사 처리 결과 응답 =====
    @Getter
    @Builder
    public static class ApprovalResultResponse {
        private Long userId;
        private String name;
        private CounselorProfile.ApprovalStatus status;
        private LocalDateTime processedAt;
        private String message;
    }
}
