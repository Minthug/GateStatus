package com.example.GateStatus.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class openApiConfig {

    @Bean
    public OpenAPI gateStatusOpenAPI() {
        return new OpenAPI()
                .openapi("3.0.1")
                .info(new Info()
                        .title("GateStatus API")
                        .description("대한민국 정치인 활동 추적 및 분석 플랫폼 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("GateStatus team")
                                .email("jkisimmortal4@gmail.com")
                                .url("https://gatestatus.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("GateStatus 프로젝트 문서")
                        .url("https://github.com/gatestatus/docs"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("개발 서버"),
                        new Server()
                                .url("https://api.gatestatus.com")
                                .description("운영 서버")
                ))
                .components(new Components()
                        .securitySchemes(Map.of(
                                // HTTP Basic Auth (관리자 API용)
                                "basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("관리자 계정으로 HTTP Basic Authentication"),

                                // 세션 인증 (브라우저 로그인용)
                                "sessionAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("세션 기반 인증 (로그인 후 자동 설정)")
                        )));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("1. Public APIs")
                .displayName("공개 API")
                .pathsToMatch("/v1/figures/**", "/v1/statements/**", "/v1/votes/**", "/v1/issues/**")
                .pathsToExclude("/v1/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi comparisonApi() {
        return GroupedOpenApi.builder()
                .group("2. Analysis APIs")
                .displayName("분석 API")
                .pathsToMatch("/v1/compares/**", "/v1/dashboard/**", "/v1/timeline/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("3. Admin APIs")
                .displayName("관리자 API")
                .pathsToMatch("/v1/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("4. System APIs")
                .displayName("시스템 API")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
