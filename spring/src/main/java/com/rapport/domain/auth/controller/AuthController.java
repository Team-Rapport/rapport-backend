package com.rapport.domain.auth.controller;

import com.rapport.domain.auth.dto.AuthDto;
import com.rapport.domain.auth.service.AuthService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 상담사 회원가입 (이메일/비밀번호)
     * 내담자는 소셜 로그인 전용이므로 이 엔드포인트는 상담사 전용
     */
    @Operation(summary = "상담사 회원가입", description = "상담사 계정 생성. 가입 후 PENDING 상태로 심사 대기.")
    @PostMapping("/counselor/signup")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> counselorSignup(
            @Valid @RequestBody AuthDto.CounselorSignupRequest request) {
        AuthDto.TokenResponse response = authService.counselorSignup(request);
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다. 관리자 심사 후 서비스를 이용하실 수 있습니다.", response));
    }

    /**
     * Refresh Token → Access Token 재발급
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새 Access Token 발급 (Rotation)")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refreshToken(
            @Valid @RequestBody AuthDto.TokenRefreshRequest request) {
        AuthDto.TokenResponse response = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", response));
    }

    /**
     * 로그아웃 — Refresh Token DB 삭제
     */
    @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다."));
    }

    /**
     * 현재 로그인 사용자 정보 확인
     */
    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        AuthDto.UserInfo userInfo = AuthDto.UserInfo.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .role(principal.getRole())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }
}
