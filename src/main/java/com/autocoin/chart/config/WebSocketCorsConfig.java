package com.autocoin.chart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * WebSocket 및 SockJS를 위한 CORS 설정
 * - 자격 증명 허용
 * - 프론트엔드 Origin 허용
 */
@Configuration
public class WebSocketCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 자격 증명 허용
        config.setAllowCredentials(true);
        
        // 허용할 Origin
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // 허용할 헤더
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // 허용할 메서드
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 노출할 헤더
        config.setExposedHeaders(Arrays.asList("Authorization"));
        
        // 최대 캐시 시간
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/ws/**", config); // WebSocket 경로에 적용
        source.registerCorsConfiguration("/api/**", config); // API 경로에도 적용
        
        return new CorsFilter(source);
    }
}
