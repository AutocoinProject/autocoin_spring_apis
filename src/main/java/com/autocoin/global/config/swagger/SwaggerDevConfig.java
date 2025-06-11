package com.autocoin.global.config.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger UI 개발 환경에서의 자동 인증 기능을 추가하는 설정 클래스
 * local 프로필에서만 활성화됩니다.
 */
@Configuration
@Profile("local")
public class SwaggerDevConfig implements WebMvcConfigurer {

    @Value("${server.port:8080}")
    private String serverPort;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI 자동 인증 스크립트를 정적 리소스로 제공
        registry.addResourceHandler("/swagger-ui/swagger-auto-auth.js")
                .addResourceLocations("classpath:/static/");
    }
}
