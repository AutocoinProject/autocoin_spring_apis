package com.autocoin.global.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * API 문서화 통합 설정
 * Swagger + OpenAPI 통합 관리
 */
@Configuration
@RequiredArgsConstructor
public class ApiDocumentationConfig {

    private final Environment environment;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        // 환경별 서버 설정
        boolean isDevelopment = Arrays.asList(environment.getActiveProfiles()).contains("local");
        
        // 기본 정보 설정
        Info info = new Info()
                .title("Autocoin API")
                .description("암호화폐 자동매매 플랫폼 API 문서")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("AutoCoin Team")
                        .email("support@autocoin.com")
                        .url("https://autocoin.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

        // 보안 스키마 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT 토큰을 입력하세요. Bearer 접두사는 자동으로 추가됩니다.");

        // 서버 정보 추가
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Server");

        Server devServer = new Server()
                .url("https://dev-api.autocoin.com")
                .description("Development Server");

        return new OpenAPI()
                .info(info)
                .servers(Arrays.asList(localServer, devServer))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme));
    }
    
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0. 전체 API")
                .displayName("전체 API")
                .pathsToMatch("/api/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
                .group("1. 로그인/회원가입 API")
                .displayName("로그인/회원가입 API")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("2. 프로필/사용자 관리 API")
                .displayName("프로필/사용자 관리 API")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("3. 게시글 관리 API")
                .displayName("게시글 관리 API")
                .pathsToMatch("/api/v1/posts/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi categoryApi() {
        return GroupedOpenApi.builder()
                .group("4. 카테고리 관리 API")
                .displayName("카테고리 관리 API")
                .pathsToMatch("/api/v1/categories/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi newsApi() {
        return GroupedOpenApi.builder()
                .group("5. 뉴스 API")
                .displayName("뉴스 API")
                .pathsToMatch("/api/v1/news/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("6. 파일 업로드/관리 API")
                .displayName("파일 업로드/관리 API")
                .pathsToMatch("/api/v1/files/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi upbitApi() {
        return GroupedOpenApi.builder()
                .group("7. 업비트 API")
                .displayName("업비트 API")
                .pathsToMatch("/api/v1/upbit/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi chartApi() {
        return GroupedOpenApi.builder()
                .group("8. 차트 API")
                .displayName("차트 API")
                .pathsToMatch("/api/v1/chart/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi tradingApi() {
        return GroupedOpenApi.builder()
                .group("9. 거래 API")
                .displayName("거래 API")
                .pathsToMatch("/api/v1/trading/**")
                .build();
    }
}
