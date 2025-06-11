package com.autocoin.config;

import com.amazonaws.services.s3.AmazonS3;
import com.autocoin.file.application.service.S3UploaderInterface;
import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.global.util.PasswordEncoderUtil;
import com.autocoin.news.config.NewsApiConfig;
import com.autocoin.notification.service.SlackNotificationService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 통합 테스트용 Configuration 클래스
 * 모든 테스트에서 필요한 Mock Bean들을 중앙 집중식으로 관리
 */
@TestConfiguration
@Profile({"test", "webmvc"})
public class TestConfig {

    /**
     * 테스트용 PasswordEncoder Bean
     * 실제 BCrypt를 사용하여 테스트 환경에서도 정상적인 암호화 수행
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 테스트용 Mock AmazonS3 Bean
     * AWS S3 연결 없이 테스트 가능
     */
    @Bean
    @Primary
    public AmazonS3 amazonS3() {
        return Mockito.mock(AmazonS3.class);
    }

    /**
     * 테스트용 Mock S3Uploader Bean
     * 파일 업로드 기능을 Mock으로 대체
     */
    @Bean
    @Primary
    public S3UploaderInterface s3Uploader() {
        return Mockito.mock(S3UploaderInterface.class);
    }

    /**
     * 테스트용 Mock SlackNotificationService Bean
     * Slack 알림 서비스를 Mock으로 대체
     */
    @Bean
    @Primary
    public SlackNotificationService slackNotificationService() {
        return Mockito.mock(SlackNotificationService.class);
    }

    /**
     * 테스트용 Mock NewsApiConfig Bean
     * 뉴스 API 설정을 Mock으로 대체
     */
    @Bean
    @Primary
    public NewsApiConfig newsApiConfig() {
        return Mockito.mock(NewsApiConfig.class);
    }

    /**
     * 테스트용 실제 PasswordEncoderUtil Bean
     * 실제 BCrypt를 사용하여 테스트 환경에서도 정상적인 암호화 수행
     */
    @Bean
    @Primary
    public PasswordEncoderUtil passwordEncoderUtil() {
        return new PasswordEncoderUtil(passwordEncoder());
    }
}
