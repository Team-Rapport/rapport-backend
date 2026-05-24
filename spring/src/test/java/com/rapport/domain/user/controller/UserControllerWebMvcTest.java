package com.rapport.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapport.domain.auth.dto.AuthDto;
import com.rapport.domain.auth.service.AuthService;
import com.rapport.domain.auth.service.CustomOAuth2UserService;
import com.rapport.domain.auth.service.OAuth2AuthenticationFailureHandler;
import com.rapport.domain.auth.service.OAuth2AuthenticationSuccessHandler;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.report.entity.ReportRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.domain.user.service.UserService;
import com.rapport.global.config.CorsConfig;
import com.rapport.global.config.SecurityConfig;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.util.JwtTokenProvider;
import com.rapport.global.util.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        properties = {
                "GOOGLE_CLIENT_ID=test-google-client-id",
                "GOOGLE_CLIENT_SECRET=test-google-client-secret",
                "KAKAO_CLIENT_ID=test-kakao-client-id",
                "KAKAO_CLIENT_SECRET=test-kakao-client-secret"
        }
)
@Import({SecurityConfig.class, CorsConfig.class})
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private BookingRepository bookingRepository;
    @MockBean
    private ReportRepository reportRepository;
    @MockBean
    private S3Service s3Service;
    @MockBean
    private AuthService authService;
    @MockBean
    private UserService userService;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    @MockBean
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("PATCH /api/v1/users/me/profile - 성공")
    void updateMyProfile_success() throws Exception {
        User user = User.createOAuthUser("client@test.com", "테스트", null, User.Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        AuthDto.UserInfo userInfo = AuthDto.UserInfo.builder()
                .id(1L)
                .email("client@test.com")
                .name("홍길동")
                .role("CLIENT")
                .profileImageUrl(null)
                .isNewUser(true)
                .profileCompleted(false)
                .onboardingCompleted(false)
                .build();

        when(authService.getMe(1L)).thenReturn(userInfo);

        String body = """
                {
                  "name": "  홍길동  ",
                  "phone": "010-1234-5678"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.profileCompleted").value(false))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false));

        verify(userService).updateMyProfile(
                eq(1L),
                eq("홍길동"),
                eq("010-1234-5678"),
                eq(null),
                eq(null)
        );
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/profile - validation 실패")
    void updateMyProfile_validationFail() throws Exception {
        User user = User.createOAuthUser("client@test.com", "테스트", null, User.Role.CLIENT);
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        String body = """
                {
                  "name": "   ",
                  "phone": "01012345678"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/profile - 인증 없음")
    void updateMyProfile_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("name", "홍길동");
            put("phone", "010-1234-5678");
        }});

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
