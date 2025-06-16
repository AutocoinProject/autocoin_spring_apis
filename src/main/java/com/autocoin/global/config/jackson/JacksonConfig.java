package com.autocoin.global.config.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson JSON 변환 설정
 * 
 * Java 8 날짜/시간 타입(LocalDateTime 등)을 JSON으로 올바르게 직렬화/역직렬화하기 위한 설정
 * ErrorResponse 객체의 timestamp 변환 오류를 해결합니다.
 */
@Configuration
public class JacksonConfig {
    
    /**
     * ObjectMapper 빈 구성
     * 
     * 1. JavaTimeModule을 등록하여 Java 8 날짜/시간 타입 지원
     * 2. WRITE_DATES_AS_TIMESTAMPS 비활성화하여 ISO-8601 형식으로 날짜 출력
     * 3. Primary 빈으로 설정하여 기본 ObjectMapper 대체
     * 
     * @return 구성된 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
    
    /**
     * Jackson2ObjectMapperBuilder 설정 (대안)
     * 
     * Spring Boot의 Jackson 자동 구성을 위한 빌더 설정
     * 
     * @return 구성된 Jackson2ObjectMapperBuilder
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modulesToInstall(new JavaTimeModule());
    }
}
