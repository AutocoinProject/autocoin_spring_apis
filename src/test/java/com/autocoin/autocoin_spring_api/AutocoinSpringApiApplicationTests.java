package com.autocoin.autocoin_spring_api;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestEntityConfig;
import com.autocoin.config.TestSchedulingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 애플리케이션 통합 테스트
 * 전체 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestConfig.class, TestEntityConfig.class, TestSchedulingConfig.class})
class AutocoinSpringApiApplicationTests {

	/**
	 * 애플리케이션 컨텍스트 로드 테스트
	 * Spring Boot 애플리케이션이 정상적으로 시작되는지 확인
	 */
	@Test
	void contextLoads() {
		// 애플리케이션 컨텍스트가 성공적으로 로드되면 테스트 통과
	}
}
