package com.autocoin.config;

import com.amazonaws.services.s3.AmazonS3;
import com.autocoin.file.application.service.S3UploaderInterface;
import com.autocoin.notification.service.SlackNotificationService;
import com.autocoin.upbit.infrastructure.UpbitApiClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트를 위한 포괄적인 Bean 설정
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 테스트용 AmazonS3 Mock - 메인 설정의 amazonS3Client를 오버라이드
     */
    @Bean("amazonS3Client")
    @Primary
    @Profile("test")
    public AmazonS3 amazonS3Client() {
        AmazonS3 mockS3 = Mockito.mock(AmazonS3.class);
        
        // Mock 동작 설정
        Mockito.when(mockS3.doesBucketExistV2(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockS3.getBucketLocation(Mockito.anyString())).thenReturn("us-east-1");
        
        return mockS3;
    }

    @Bean
    @Primary
    public S3UploaderInterface s3UploaderInterface() {
        S3UploaderInterface mock = Mockito.mock(S3UploaderInterface.class);
        
        // 기본 동작 설정
        try {
            Mockito.when(mock.upload(Mockito.any(), Mockito.anyString()))
                   .thenReturn("https://test-bucket.s3.amazonaws.com/test-file.jpg");
            Mockito.when(mock.checkS3Connection()).thenReturn(true);
            Mockito.doNothing().when(mock).delete(Mockito.anyString());
        } catch (Exception e) {
            // Mock 설정 중 예외 처리
        }
        
        return mock;
    }

    @Bean
    @Primary
    public SlackNotificationService slackNotificationService() {
        SlackNotificationService mock = Mockito.mock(SlackNotificationService.class);
        
        // Mock 동작 설정 - 실제 메서드 시그니처에 맞춤
        try {
            Mockito.doNothing().when(mock).sendMessage(Mockito.anyString());
            Mockito.doNothing().when(mock).sendMessage(Mockito.anyString(), Mockito.anyString());
            Mockito.doNothing().when(mock).sendErrorMessage(Mockito.anyString(), Mockito.any(Throwable.class));
            Mockito.doNothing().when(mock).sendErrorNotification(Mockito.anyString(), Mockito.anyString(), Mockito.any(Exception.class));
            Mockito.doNothing().when(mock).sendInfoNotification(Mockito.anyString(), Mockito.anyString());
            Mockito.doNothing().when(mock).sendSuccessNotification(Mockito.anyString(), Mockito.anyString());
            Mockito.doNothing().when(mock).sendWarningNotification(Mockito.anyString(), Mockito.anyString());
            Mockito.doNothing().when(mock).sendTradeNotification(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
            Mockito.doNothing().when(mock).sendStartupNotification();
            Mockito.doNothing().when(mock).sendShutdownNotification();
            Mockito.doNothing().when(mock).sendHealthCheckNotification(Mockito.anyString(), Mockito.anyMap());
        } catch (Exception e) {
            // Mock 설정 중 예외 처리
        }
        
        return mock;
    }

    @Bean
    @Primary
    public UpbitApiClient upbitApiClient() {
        UpbitApiClient mock = Mockito.mock(UpbitApiClient.class);
        
        // Mock 동작 설정 - 실제 메서드 시그니처에 맞춤
        try {
            // 빈 리스트나 맵 반환
            List<Map<String, Object>> emptyList = new ArrayList<>();
            Map<String, Object> emptyMap = new HashMap<>();
            
            Mockito.when(mock.getMarkets()).thenReturn(emptyList);
            Mockito.when(mock.getAccounts(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
            Mockito.when(mock.getTickers(Mockito.anyList())).thenReturn(new ArrayList<>());
            Mockito.when(mock.getOrderbook(Mockito.anyList())).thenReturn(emptyList);
            Mockito.when(mock.getDayCandles(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(emptyList);
            Mockito.when(mock.getMinuteCandles(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(emptyList);
            Mockito.when(mock.getTrades(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString())).thenReturn(emptyList);
        } catch (Exception e) {
            // Mock 설정 중 예외 처리
        }
        
        return mock;
    }
}
