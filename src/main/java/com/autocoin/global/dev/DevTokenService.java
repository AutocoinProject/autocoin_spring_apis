package com.autocoin.global.dev;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

/**
 * 개발용 JWT 토큰 생성 서비스
 * 로컬 환경에서만 활성화됩니다.
 */
@Service
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DevTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${dev.jwt.user.id:1}")
    private Long devUserId;
    
    @Value("${dev.jwt.user.email:dev@autocoin.com}")
    private String devUserEmail;
    
    @Value("${dev.jwt.user.role:ADMIN}")
    private String devUserRole;
    
    /**
     * 개발용 장기 토큰 생성 (1년 유효)
     */
    public String generateLongTermDevToken() {
        // 1년 후 만료
        LocalDateTime expiry = LocalDateTime.now().plusYears(1);
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());
        
        String token = jwtTokenProvider.createTokenWithCustomExpiry(
            devUserId, 
            devUserEmail, 
            Arrays.asList(devUserRole),
            expiryDate
        );
        
        log.info("개발용 장기 토큰 생성 완료 - 사용자: {}, 만료일: {}", devUserEmail, expiry);
        return token;
    }
    
    /**
     * 개발용 단기 토큰 생성 (기본 30분)
     */
    public String generateShortTermDevToken() {
        String token = jwtTokenProvider.createToken(
            devUserId, 
            devUserEmail, 
            Arrays.asList(devUserRole)
        );
        
        log.info("개발용 단기 토큰 생성 완료 - 사용자: {}", devUserEmail);
        return token;
    }
    
    /**
     * 현재 설정된 개발용 사용자 정보 반환
     */
    public DevUserInfo getDevUserInfo() {
        return DevUserInfo.builder()
            .id(devUserId)
            .email(devUserEmail)
            .username("개발자")
            .role(devUserRole)
            .build();
    }
}
