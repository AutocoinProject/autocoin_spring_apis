package com.autocoin.config;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.user.application.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 환경용 JWT Configuration
 */
@TestConfiguration
@Profile({"test", "webmvc"})
public class TestJwtConfig {

    /**
     * 테스트용 JwtTokenProvider Bean
     * 실제 JWT 토큰 생성 및 검증 기능을 제공
     */
    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider(UserService userService) {
        return new JwtTokenProvider(userService);
    }
}
