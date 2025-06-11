package com.autocoin.global.config.monitoring;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import io.sentry.spring.jakarta.SentryUserProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enhanced Sentry 오류 추적 설정
 * 예외 발생 시 실시간 알림과 추적을 제공합니다.
 */
@Slf4j
@Configuration
public class SentryConfig {

    @Value("${sentry.dsn:}")
    private String sentryDsn;

    @Value("${sentry.environment:local}")
    private String environment;

    @Value("${sentry.release:1.0.0}")
    private String release;

    @Value("${sentry.debug:false}")
    private Boolean debug;

    @Value("${sentry.attach-threads:false}")
    private Boolean attachThreads;

    @Value("${sentry.traces-sample-rate:0.1}")
    private Double tracesSampleRate;

    @PostConstruct
    public void init() {
        System.out.println("===========================================");
        System.out.println("SentryConfig @PostConstruct 실행됨!");
        System.out.println("sentryDsn from @Value: " + sentryDsn);
        System.out.println("===========================================");
        
        log.info("=== Sentry 초기화 시작 ===");
        log.info("Sentry DSN from properties: {}", sentryDsn);
        
        // DSN이 있고 "disabled"가 아닌 경우에만 Sentry 활성화
        if (sentryDsn != null && !sentryDsn.isEmpty() && !"disabled".equals(sentryDsn)) {
            try {
                // Sentry 수동 초기화
                SentryOptions options = new SentryOptions();
                options.setDsn(sentryDsn);
                options.setEnvironment(environment);
                options.setRelease(release);
                options.setTracesSampleRate(tracesSampleRate);
                options.setAttachStacktrace(true);
                options.setAttachThreads(attachThreads); // 환경변수로 제어
                options.setEnableTracing(true);
                options.setDebug(debug); // 환경변수로 제어
                
                // 민감한 정보 필터링
                options.setBeforeSend((event, hint) -> {
                    if (event.getRequest() != null && event.getRequest().getHeaders() != null) {
                        event.getRequest().getHeaders().remove("Authorization");
                        event.getRequest().getHeaders().remove("Cookie");
                    }
                    return event;
                });
                
                // Sentry 초기화
                Sentry.init(options);
                
                System.out.println("Sentry.init() 호출 완료!");
                System.out.println("Sentry.isEnabled(): " + Sentry.isEnabled());
                
                log.info("=== Sentry 초기화 완료 ===");
                log.info("Sentry DSN configured: {}", sentryDsn.substring(0, Math.min(20, sentryDsn.length())) + "...");
                log.info("Sentry Environment: {}", environment);
                log.info("Sentry Release: {}", release);
                log.info("Sentry Enabled: {}", Sentry.isEnabled());
                log.info("Sentry Hub: {}", Sentry.getCurrentHub());
                log.info("=============================");
                
                // 초기화 테스트 메시지 전송
                Sentry.captureMessage("Sentry 초기화 완료 - 테스트 메시지");
                
            } catch (Exception e) {
                System.out.println("Sentry 초기화 중 예외 발생: " + e.getMessage());
                e.printStackTrace();
                log.error("Sentry 초기화 중 오류 발생: {}", e.getMessage(), e);
            }
        } else {
            System.out.println("Sentry DSN이 설정되지 않음: '" + sentryDsn + "'");
            log.info("Sentry DSN이 설정되지 않았습니다. 에러 트래킹이 비활성화됩니다.");
        }
    }

    /**
     * Sentry 사용자 정보 제공자
     */
    @Bean
    public SentryUserProvider sentryUserProvider() {
        return () -> {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() 
                    && !"anonymousUser".equals(authentication.getName())) {
                    User user = new User();
                    user.setUsername(authentication.getName());
                    user.setId(authentication.getName());
                    return user;
                }
            } catch (Exception e) {
                log.warn("Failed to get user information for Sentry: {}", e.getMessage());
            }
            return null;
        };
    }
}
