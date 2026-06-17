package com.rapport.domain.auth.service;

import com.rapport.domain.auth.dto.AuthDto;
import com.rapport.domain.auth.entity.RefreshTokenRepository;
import com.rapport.domain.chat.entity.AiChatSession;
import com.rapport.domain.chat.entity.AiChatSessionRepository;
import com.rapport.domain.counselor.entity.CounselorCredentialRepository;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.util.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CounselorProfileRepository counselorProfileRepository;
    @Mock
    private CounselorCredentialRepository counselorCredentialRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AiChatSessionRepository aiChatSessionRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("getMe - profile/onboarding 상태값 반영")
    void getMe_reflectsProfileAndOnboardingStatus() {
        User user = User.createOAuthUser("client@test.com", "홍길동", null, User.Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", 1L);
        user.updateProfile("홍길동", "010-1234-5678", User.Gender.MALE, LocalDate.of(1998, 1, 1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(aiChatSessionRepository.existsByClientIdAndStatus(1L, AiChatSession.SessionStatus.COMPLETED))
                .thenReturn(true);

        AuthDto.UserInfo me = authService.getMe(1L);

        assertThat(me.isProfileCompleted()).isTrue();
        assertThat(me.isOnboardingCompleted()).isTrue();
        assertThat(me.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("getMe - COUNSELOR 상태값(approvalStatus, credentialsSubmitted) 반영")
    void getMe_reflectsCounselorStatusFields() {
        User user = User.createCounselorUser("counselor@test.com", "encoded", "상담사");
        ReflectionTestUtils.setField(user, "id", 28L);

        CounselorProfile profile = CounselorProfile.create(
                user, "UNSPECIFIED", null, CounselorProfile.CounselorGender.ANY);

        when(userRepository.findById(28L)).thenReturn(Optional.of(user));
        when(counselorProfileRepository.findByUserId(28L)).thenReturn(Optional.of(profile));
        when(counselorCredentialRepository.existsByCounselorId(28L)).thenReturn(false);

        AuthDto.UserInfo me = authService.getMe(28L);

        assertThat(me.getApprovalStatus()).isEqualTo("PENDING");
        assertThat(me.isCredentialsSubmitted()).isFalse();
    }
}
