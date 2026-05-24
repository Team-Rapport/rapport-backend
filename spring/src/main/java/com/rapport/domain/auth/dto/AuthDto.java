package com.rapport.domain.auth.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

public class AuthDto {

    // ===== 상담사 회원가입 요청 =====
    @Getter
    public static class CounselorSignupRequest {

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255)
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        private String password;

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름 형식이 올바르지 않습니다.")
        private String name;

        @NotBlank(message = "자격증 종류를 입력해주세요.")
        private String licenseType;

        private String licenseNumber;
    }

    // ===== Refresh Token 재발급 요청 =====
    @Getter
    public static class TokenRefreshRequest {

        @NotBlank(message = "Refresh Token을 입력해주세요.")
        private String refreshToken;
    }

    // ===== 토큰 응답 =====
    @Getter
    @Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserInfo user;
    }

    // ===== 사용자 기본 정보 (토큰과 함께 반환) =====
    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String name;
        private String role;
        private String profileImageUrl;
        @JsonProperty("isNewUser")
        private boolean isNewUser;
        private boolean profileCompleted;
        private boolean onboardingCompleted;
    }

    @Getter
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
    }

    // ===== 이메일 인증 코드 발송 요청 =====
    @Getter
    public static class EmailSendCodeRequest {

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255)
        private String email;
    }

    // ===== 이메일 인증 코드 검증 요청 =====
    @Getter
    public static class EmailVerifyCodeRequest {

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;

        @NotBlank(message = "인증 코드를 입력해주세요.")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.")
        private String code;
    }

    // ===== 회원 탈퇴 요청 =====
    @Getter
    public static class WithdrawRequest {
        // 이메일 계정(상담사)의 경우 비밀번호 확인. 소셜 계정은 null 허용.
        private String password;
    }

    // ===== 비밀번호 변경 요청 (상담사 전용) =====
    @Getter
    public static class ChangePasswordRequest {

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        private String newPassword;
    }
}
