package com.autocoin.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * WebMvcTest 전용 JPA 설정 클래스
 * 슬라이스 테스트에서 JPA 관련 자동설정을 비활성화하여 
 * "JPA metamodel must not be empty" 오류를 방지
 */
@TestConfiguration
@Profile("webmvc")
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    TaskSchedulingAutoConfiguration.class
})
public class TestJpaConfig implements SchedulingConfigurer {
    
    /**
     * EntityManager Mock Bean
     * JPA 관련 의존성을 Mock으로 대체
     */
    @MockBean
    private jakarta.persistence.EntityManager entityManager;
    
    /**
     * EntityManagerFactory Mock Bean
     * JPA 관련 의존성을 Mock으로 대체
     */
    @MockBean
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    /**
     * 스케줄링 비활성화
     * @Scheduled 어노테이션이 테스트에서 실행되지 않도록 함
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 스케줄러를 설정하지 않아서 @Scheduled 어노테이션이 동작하지 않도록 함
    }
}
