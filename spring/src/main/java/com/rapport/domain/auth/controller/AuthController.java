package com.rapport.domain.auth.controller;

import com.rapport.domain.auth.dto.AuthDto;
import com.rapport.domain.auth.service.AuthService;
import com.rapport.domain.auth.service.EmailVerificationService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 상담사 회원가입 (이메일/비밀번호)
     * 내담자는 소셜 로그인 전용이므로 이 엔드포인트는 상담사 전용
     */
    @Operation(summary = "상담사 회원가입", description = """
            상담사 계정 생성(기본 계정 정보 단계).
            필수: email, password, name
            선택(하위호환): licenseType, licenseNumber
            가입 후 계정/프로필은 PENDING 상태로 심사 대기입니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이메일 인증 미완료/만료 또는 입력값 오류",
                    content = @Content(examples = {
                            @ExampleObject(name = "미인증", value = "{\"success\":false,\"message\":\"이메일 인증이 완료되지 않았습니다.\"}"),
                            @ExampleObject(name = "인증만료", value = "{\"success\":false,\"message\":\"인증 코드가 만료되었습니다.\"}")
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "이메일 인증 요청 없음",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"success\":false,\"message\":\"이메일 인증 요청을 찾을 수 없습니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
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
        AuthDto.UserInfo userInfo = authService.getMe(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }

    @Operation(summary = "이메일 로그인 (상담사)")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("로그인 성공", authService.login(request)));
    }

    // ===== 이메일 인증 =====

    @Operation(summary = "이메일 인증 코드 발송",
               description = "상담사 회원가입 전 이메일 인증 코드를 발송합니다. 코드 유효 시간은 10분입니다.")
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody AuthDto.EmailSendCodeRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 발송되었습니다. 이메일을 확인해주세요."));
    }

    @Operation(summary = "이메일 인증 코드 검증",
               description = "발송된 6자리 코드를 검증합니다. 이후 회원가입이 가능합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @Valid @RequestBody AuthDto.EmailVerifyCodeRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다."));
    }

    // ===== 비밀번호 변경 (상담사 전용) =====

    @Operation(summary = "비밀번호 변경",
               description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다. 기존 세션은 모두 만료됩니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/password")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다. 다시 로그인해주세요."));
    }
}
