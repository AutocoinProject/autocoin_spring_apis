package com.autocoin.global.config.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * AWS S3 Configuration
 * 보안을 위해 환경변수 기반 인증 사용
 */
@Slf4j
@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    /**
     * Primary S3 Client - 환경변수 또는 IAM 롤 기반 인증
     * 우선순위: 환경변수 -> IAM 롤 -> EC2 인스턴스 메타데이터
     */
    @Bean
    @Primary
    public AmazonS3 amazonS3Client() {
        log.info("=== AWS S3 클라이언트 초기화 시작 ===");
        log.info("AWS 설정 - 리전: {}", region);
        log.info("AWS 설정 - Access Key: {}", accessKey != null && !accessKey.isEmpty() ? accessKey.substring(0, Math.min(accessKey.length(), 8)) + "..." : "비어있음");
        log.info("AWS 설정 - Secret Key: {}", secretKey != null && !secretKey.isEmpty() ? "[설정됨]" : "비어있음");
        
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(region);

        // 환경변수에 키가 있으면 사용, 없으면 기본 provider chain 사용
        if (accessKey != null && !accessKey.isEmpty() && 
            secretKey != null && !secretKey.isEmpty()) {
            // 환경변수로 설정된 키 사용
            log.info("AWS 인증: 환경변수 기반 인증 사용");
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        } else {
            // 기본 provider chain 사용 (환경변수 -> IAM 롤 -> EC2 메타데이터)
            log.info("AWS 인증: 기본 provider chain 사용");
            builder.withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        }

        AmazonS3 s3Client = builder.build();
        log.info("=== AWS S3 클라이언트 초기화 완료 ===");
        return s3Client;
    }

    /**
     * 개발 환경용 S3 클라이언트 (환경변수 우선)
     */
    @Bean("devS3Client")
    @Profile({"local", "dev"})
    public AmazonS3 devS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();
    }

    /**
     * 운영 환경용 S3 클라이언트 (IAM 롤 우선)
     */
    @Bean("prodS3Client")
    @Profile("prod")
    public AmazonS3 prodS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }
}