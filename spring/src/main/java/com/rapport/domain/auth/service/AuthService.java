package com.rapport.domain.auth.service;

import com.rapport.domain.auth.dto.AuthDto;
import com.rapport.domain.auth.entity.RefreshToken;
import com.rapport.domain.auth.entity.RefreshTokenRepository;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CounselorProfileRepository counselorProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // ===== 상담사 회원가입 (이메일/비밀번호) =====

    @Transactional
    public AuthDto.TokenResponse counselorSignup(AuthDto.CounselorSignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // User 생성
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.createCounselorUser(request.getEmail(), encodedPassword, request.getName());
        user = userRepository.save(user);

        // CounselorProfile 생성 (PENDING 상태)
        CounselorProfile profile = CounselorProfile.create(
                user,
                request.getLicenseType(),
                request.getLicenseNumber(),
                CounselorProfile.CounselorGender.ANY
        );
        counselorProfileRepository.save(profile);

        log.info("Counselor signup: userId={}, email={}", user.getId(), user.getEmail());
        return issueTokens(user);
    }

    // ===== Refresh Token으로 Access Token 재발급 =====

    @Transactional
    public AuthDto.TokenResponse refreshTokens(String refreshTokenStr) {
        // DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // JWT 검증
        jwtTokenProvider.validateToken(refreshTokenStr);

        User user = refreshToken.getUser();

        // 기존 Refresh Token 삭제 후 새 토큰 발급 (Rotation)
        refreshTokenRepository.delete(refreshToken);
        return issueTokens(user);
    }

    // ===== 로그아웃 =====

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("User logout: userId={}", userId);
    }

    // ===== 내부: 토큰 발급 공통 메서드 =====

    @Transactional
    public AuthDto.TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        long expirationMs = jwtTokenProvider.getRefreshTokenExpirationMs();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        refreshTokenRepository.deleteAllByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.of(user, refreshTokenStr, expiresAt));

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .user(AuthDto.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .build();
    }
}
