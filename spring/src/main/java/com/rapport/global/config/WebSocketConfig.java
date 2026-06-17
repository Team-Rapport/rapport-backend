package com.rapport.global.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket(STOMP) 설정
 *
 * 연결 엔드포인트 : /ws/chat  (SockJS 지원)
 * 구독 prefix    : /topic    (브로드캐스트)
 *                  /queue    (1:1 메시지)
 * 발행 prefix    : /app      (클라이언트 → 서버)
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 인메모리 브로커 활성화 (구독 prefix)
        registry.enableSimpleBroker("/topic", "/queue");
        // 클라이언트 발행 prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 특정 사용자에게 전송할 때 prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS 폴백 지원
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 검증 인터셉터 등록
        registration.interceptors(jwtChannelInterceptor);
    }
}
