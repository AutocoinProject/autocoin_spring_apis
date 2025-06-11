package com.autocoin.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 테스트 환경에서 스케줄링을 비활성화하는 Configuration
 * 뉴스 수집 스케줄러 등이 테스트 중에 실행되지 않도록 함
 */
@TestConfiguration
@Profile({"test", "webmvc"})
public class TestSchedulingConfig implements SchedulingConfigurer {

    /**
     * 스케줄링 태스크 등록을 비워두어 모든 @Scheduled 어노테이션 비활성화
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 스케줄러를 설정하지 않아서 @Scheduled 어노테이션이 동작하지 않도록 함
    }
}
