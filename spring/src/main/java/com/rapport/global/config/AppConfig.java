package com.rapport.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 공통 빈 설정
 * PasswordEncoder를 SecurityConfig에서 분리 — 순환 참조 방지
 *
 * 순환 참조 구조:
 *   AuthService → PasswordEncoder ← SecurityConfig
 *                                 ↖ OAuth2SuccessHandler → AuthService (순환!)
 * 해결:
 *   PasswordEncoder를 AppConfig로 분리하면
 *   SecurityConfig가 AuthService 쪽 빈을 건드리지 않게 됨
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
