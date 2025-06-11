package com.autocoin.global.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2 설정을 조건부로 활성화하는 클래스
 * oauth2.enabled 프로퍼티가 true일 때만 OAuth2 설정을 활성화합니다.
 */
@Configuration
@ConditionalOnProperty(name = "oauth2.enabled", havingValue = "true", matchIfMissing = false)
public class ConditionalOAuth2Config {
    // OAuth2 관련 설정이 필요한 경우 여기에 추가
}
