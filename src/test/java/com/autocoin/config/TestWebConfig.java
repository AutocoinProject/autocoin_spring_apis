package com.autocoin.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * 테스트 환경용 웹 관련 Configuration
 */
@TestConfiguration
@Profile({"test", "webmvc"})
public class TestWebConfig {

    /**
     * 테스트용 RestTemplate Bean
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
