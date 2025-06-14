package com.autocoin;

import com.autocoin.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 기본 Spring Boot 애플리케이션 컨텍스트 로딩 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class AutocoinSpringApiApplicationTests {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로딩되는지 확인
        // 별도의 로직 없이 컨텍스트 로딩만 테스트
    }

}
