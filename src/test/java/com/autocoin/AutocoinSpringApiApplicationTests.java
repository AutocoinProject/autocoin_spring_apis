package com.autocoin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 기본 Spring Boot 애플리케이션 컨텍스트 로딩 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class,
    ReactiveOAuth2ClientAutoConfiguration.class
})
@ActiveProfiles("test")
class AutocoinSpringApiApplicationTests {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로딩되는지 확인
        // 웹 환경 없이 기본 컨텍스트만 테스트
    }

}
