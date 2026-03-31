package com.rapport.global.config;

import com.rapport.domain.auth.service.CustomOAuth2UserService;
import com.rapport.domain.auth.service.OAuth2AuthenticationFailureHandler;
import com.rapport.domain.auth.service.OAuth2AuthenticationSuccessHandler;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.context.annotation.Bean;

/**
 * Spring Security 설정
 * - PasswordEncoder 빈은 AppConfig로 분리 (순환 참조 방지)
 * - CORS는 CorsConfig의 빈을 주입받아 사용 (중복 정의 X)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource; // CorsConfig에서 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/api/health",
            "/actuator/health",
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/counselor/**").hasRole("COUNSELOR")
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint ->
                            endpoint.baseUri("/api/v1/auth/oauth2/authorize"))
                    .redirectionEndpoint(endpoint ->
                            endpoint.baseUri("/api/v1/auth/oauth2/callback/*"))
                    .userInfoEndpoint(userInfo ->
                            userInfo.userService(customOAuth2UserService))
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            )
            .addFilterBefore(
                    new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
