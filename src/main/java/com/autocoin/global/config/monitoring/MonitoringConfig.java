package com.autocoin.global.config.monitoring;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 기본 모니터링 설정
 * TimedAspect와 기본 Health/Info 기능을 제공합니다.
 */
@Slf4j
@Configuration
public class MonitoringConfig {

    private final Environment environment;

    public MonitoringConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Micrometer의 @Timed 어노테이션을 활성화하는 Aspect
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * 커스텀 헬스 체크 인디케이터
     */
    @Bean("basicHealthIndicator")
    public HealthIndicator basicHealthIndicator() {
        return () -> {
            try {
                return Health.up()
                        .withDetail("status", "healthy")
                        .withDetail("timestamp", LocalDateTime.now())
                        .withDetail("version", "1.0.0")
                        .withDetail("environment", environment.getActiveProfiles())
                        .build();
            } catch (Exception e) {
                log.error("Health check failed", e);
                return Health.down()
                        .withDetail("status", "unhealthy")
                        .withDetail("error", e.getMessage())
                        .withDetail("timestamp", LocalDateTime.now())
                        .build();
            }
        };
    }

    /**
     * 애플리케이션 정보 기여자
     */
    @Bean
    public InfoContributor appInfoContributor() {
        return (Info.Builder builder) -> {
            builder.withDetail("app", Map.of(
                    "name", "Autocoin Spring API",
                    "version", "1.0.0",
                    "description", "암호화폐 자동 투자 플랫폼 API",
                    "build-time", LocalDateTime.now(),
                    "java-version", System.getProperty("java.version"),
                    "active-profiles", environment.getActiveProfiles()
            ));
        };
    }
}
