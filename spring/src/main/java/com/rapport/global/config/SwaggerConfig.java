package com.rapport.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(SpringDoc OpenAPI) 설정
 * JWT Bearer 토큰 인증 스키마를 포함합니다.
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_TOKEN_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증 스키마 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(BEARER_TOKEN_SCHEME);

        // 전역 보안 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(BEARER_TOKEN_SCHEME);

        return new OpenAPI()
                .info(new Info()
                        .title("Rapport API")
                        .description("AI 기반 심리 상담 사전 점검 & 상담사 매칭 플랫폼 API")
                        .version("v1.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_TOKEN_SCHEME, securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
