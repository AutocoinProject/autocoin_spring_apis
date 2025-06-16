package com.autocoin.global.config.security;

import com.autocoin.user.oauth.CustomOAuth2UserService;
import com.autocoin.user.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 로그인 설정을 담당하는 클래스
 * oauth2.enabled 프로퍼티가 true일 때만 활성화됩니다.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oauth2.enabled", havingValue = "true", matchIfMissing = false)
public class OAuth2SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    /**
     * OAuth2 로그인이 활성화된 경우의 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        // OAuth2 로그인 설정 추가
        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                // OAuth2 로그인 페이지 경로 설정
                .authorizationEndpoint(authorization -> authorization
                        .baseUri("/oauth2/authorization"))
        );
        
        return http.build();
    }
}
