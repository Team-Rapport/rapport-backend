package com.rapport.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Configuration
public class SwaggerConfig {

    // ★ Pageable → Springdoc 전용 Pageable로 교체 (무한 로딩 방지)
    static {
        SpringDocUtils.getConfig().replaceWithClass(
                Pageable.class,
                org.springdoc.core.converters.models.Pageable.class
        );
    }

    private static final String BEARER_TOKEN_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(BEARER_TOKEN_SCHEME);

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(BEARER_TOKEN_SCHEME);

        return new OpenAPI()
                .info(new Info()
                        .title("Rapport API")
                        .description("AI 기반 심리 상담 사전 점검 & 상담사 매칭 플랫폼 API")
                        .version("v1.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 개발 서버")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_TOKEN_SCHEME, securityScheme))
                .addSecurityItem(securityRequirement);
    }
}