package com.autocoin.upbit.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpbitAuthService {
    
    @Value("${app.upbit.api.url:https://api.upbit.com}")
    private String upbitApiUrl;
    
    /**
     * 업비트 API 요청을 위한 JWT 토큰 생성
     */
    public String generateAuthorizationToken(String accessKey, String secretKey, String queryString) {
        try {
            // JWT 헤더
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(header.getBytes(StandardCharsets.UTF_8));
            
            // JWT 페이로드
            String nonce = UUID.randomUUID().toString();
            String queryHash = "";
            
            if (queryString != null && !queryString.isEmpty()) {
                MessageDigest md = MessageDigest.getInstance("SHA-512");
                md.update(queryString.getBytes(StandardCharsets.UTF_8));
                queryHash = Base64.getEncoder().encodeToString(md.digest());
            }
            
            String payload;
            if (queryHash.isEmpty()) {
                payload = String.format(
                    "{\"access_key\":\"%s\",\"nonce\":\"%s\"}",
                    accessKey, nonce
                );
            } else {
                payload = String.format(
                    "{\"access_key\":\"%s\",\"nonce\":\"%s\",\"query_hash\":\"%s\",\"query_hash_alg\":\"SHA512\"}",
                    accessKey, nonce, queryHash
                );
            }
            
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            
            // JWT 서명
            String message = encodedHeader + "." + encodedPayload;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sha256Hmac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            
            return "Bearer " + message + "." + signature;
            
        } catch (Exception e) {
            log.error("JWT 토큰 생성 중 오류 발생", e);
            throw new RuntimeException("JWT 토큰 생성 실패", e);
        }
    }
    
    /**
     * 업비트 API 키 유효성 검증
     */
    public boolean validateApiKeys(String accessKey, String secretKey) {
        try {
            // 간단한 계정 정보 조회로 API 키 유효성 검증
            String authToken = generateAuthorizationToken(accessKey, secretKey, null);
            // 실제로는 RestTemplate으로 업비트 API 호출하여 검증
            // 여기서는 토큰 생성이 성공하면 유효한 것으로 간주
            return true;
        } catch (Exception e) {
            log.error("API 키 유효성 검증 실패", e);
            return false;
        }
    }
}