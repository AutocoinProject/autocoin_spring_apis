package com.autocoin.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 테스트 환경에서 Entity와 Repository 스캔 설정
 * @SpringBootTest 통합 테스트에서 사용
 */
@TestConfiguration
@Profile("test")
@EntityScan(basePackages = {
    "com.autocoin.user.domain",
    "com.autocoin.post.domain.entity",
    "com.autocoin.category.domain.entity",
    "com.autocoin.news.domain.entity",
    "com.autocoin.upbit.domain.entity",
    "com.autocoin.file.domain",
    "com.autocoin.chart.domain.entity"
})
@EnableJpaRepositories(
    basePackages = {
        "com.autocoin.user.infrastructure",
        "com.autocoin.post.infrastructure.repository",
        "com.autocoin.category.domain",
        "com.autocoin.news.infrastructure.repository",
        "com.autocoin.upbit.infrastructure",
        "com.autocoin.file.infrastructure",
        "com.autocoin.chart.domain.repository"
    },
    includeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX, 
        pattern = ".*Repository.*"
    )
)
public class TestEntityConfig {
    // Entity와 Repository 스캔을 위한 설정 클래스
}
