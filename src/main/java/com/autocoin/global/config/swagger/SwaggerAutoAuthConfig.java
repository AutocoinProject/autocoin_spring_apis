package com.autocoin.global.config.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger UI 자동 인증 스크립트를 추가하기 위한 설정 클래스
 * local 프로필에서만 활성화됩니다.
 */
@Configuration
@Profile("local")
public class SwaggerAutoAuthConfig implements WebMvcConfigurer {

    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI 자동 인증 스크립트를 정적 리소스로 제공
        registry.addResourceHandler("/swagger-ui/swagger-auto-auth.js")
                .addResourceLocations("classpath:/static/");
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Swagger UI 리다이렉션 설정
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui/index.html");
        
        // 자동 인증 스크립트를 포함하는 커스텀 Swagger UI 리다이렉션
        registry.addViewController("/swagger-ui/")
                .setViewName("forward:/swagger-ui/index.html");
    }
    
    /**
     * Swagger UI의 HTML에 자동 인증 스크립트를 포함시키는 커스텀 필터
     * 응답 충돌 문제로 인해 임시 비활성화
     */
    // @Bean
    // @Profile("local")
    // public SwaggerUICustomFilter swaggerUICustomFilter() {
    //     return new SwaggerUICustomFilter();
    // }
}
