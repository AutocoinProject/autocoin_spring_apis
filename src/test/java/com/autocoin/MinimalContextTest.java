package com.autocoin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 간단한 Spring Boot 테스트
 * OAuth2 설정을 완전히 우회하는 컨텍스트 로드 테스트
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration",
        "oauth2.enabled=false",
        "spring.security.oauth2.client.registration=",
        "spring.security.oauth2.client.provider="
    }
)
@ActiveProfiles("test")
public class MinimalContextTest {

    @Test
    void contextLoads() {
        // 기본 컨텍스트 로드 테스트
        System.out.println("✅ Spring Context loaded successfully without OAuth2!");
    }
}
