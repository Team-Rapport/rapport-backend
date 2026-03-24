package com.rapport.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 매 요청마다 Authorization 헤더에서 JWT 토큰을 추출하고 검증합니다.
 * 실제 토큰 파싱 로직은 auth 도메인 구현 시 완성할 예정입니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // TODO: Authorization 헤더에서 Bearer 토큰 추출
        // String authHeader = request.getHeader("Authorization");
        // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        //     filterChain.doFilter(request, response);
        //     return;
        // }
        // String token = authHeader.substring(7);

        // TODO: JwtTokenProvider를 통해 토큰 유효성 검증
        // if (jwtTokenProvider.validateToken(token)) {

        //     TODO: 토큰에서 사용자 정보 추출 후 SecurityContext에 인증 정보 저장
        //     Authentication authentication = jwtTokenProvider.getAuthentication(token);
        //     SecurityContextHolder.getContext().setAuthentication(authentication);
        // }

        filterChain.doFilter(request, response);
    }
}
