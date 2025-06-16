package com.autocoin.global.config.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 설정 클래스
 * 이 설정을 분리함으로써 @WebMvcTest와 같은 슬라이스 테스트에서 
 * JPA Auditing으로 인한 오류를 방지할 수 있습니다.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = "com.autocoin",
    enableDefaultTransactions = true,
    namedQueriesLocation = "classpath*:META-INF/jpa-named-queries.properties",
    considerNestedRepositories = true
)
public class JpaAuditingConfig {

    /**
     * 현재 로그인한 사용자를 엔티티의 생성자/수정자로 자동 지정하는 AuditorAware 구현
     * SecurityContext에서 인증 정보를 가져와 사용자 이름을 제공합니다.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}
