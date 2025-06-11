package com.autocoin.global.auth.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenValidTime; // Token expiration time in milliseconds
    
    private final UserDetailsService userDetailsService;
    
    private Key key;

    // Constructor with @Lazy
    public JwtTokenProvider(@Lazy UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // Initialize the key using the secret from application.yml
    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // Create JWT token with user_id, email and expiration time
    public String createToken(Long userId, String email, List<String> roles) {
        Claims claims = Jwts.claims();
        claims.put("user_id", userId);
        claims.put("email", email);
        claims.put("roles", roles); // Optional: include roles if needed
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenValidTime);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Get authentication information from token
    public Authentication getAuthentication(String token) {
        // 토큰에서 이메일과 userId 추출
        String email = getEmail(token);
        Long userId = getUserId(token);
        
        log.debug("JWT 토큰에서 추출한 정보: email={}, userId={}", email, userId);
        
        // UserDetails 서비스를 통해 사용자 정보 로드
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        // 디버깅 로그 추가
        if (userDetails != null) {
            log.debug("인증 정보 생성 성공: email={}, principal={}", email, userDetails.getClass().getSimpleName());
            log.debug("사용자 권한: {}", userDetails.getAuthorities());
        } else {
            log.warn("인증 정보 생성 실패: 해당 이메일로 사용자를 찾을 수 없습니다: {}", email);
        }
        
        // Authentication 객체에서 getName()이 email을 반환하도록 설정
        return new UsernamePasswordAuthenticationToken(email, "", userDetails.getAuthorities());
    }

    // Extract email from token
    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("email", String.class);
    }

    // Extract user_id from token
    public Long getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("user_id", Long.class);
    }

    // Extract token from request
    public String resolveToken(HttpServletRequest request) {
        // 기존 Authorization 헤더 체크
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null) {
            // Bearer 접두사가 있으면 제거
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            // Bearer 접두사 없이 직접 토큰이 전달된 경우
            else {
                log.debug("Authorization 헤더에서 토큰 추출 (비 Bearer 형식): {}", 
                        bearerToken.length() > 20 ? bearerToken.substring(0, 20) + "..." : bearerToken);
                return bearerToken;
            }
        }
        
        // authorization-token 헤더 체크
        String altToken = request.getHeader("authorization-token");
        if (altToken != null && !altToken.isEmpty()) {
            log.debug("authorization-token 헤더에서 토큰 추출: {}...", 
                    altToken.length() > 20 ? altToken.substring(0, 20) : altToken);
            return altToken;
        }
        
        // 둘 다 없는 경우
        log.debug("유효한 인증 헤더를 찾을 수 없음");
        return null;
    }

    // Validate token
    public boolean validateToken(String token) {
        log.debug("토큰 검증 시작: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
        
        try {
            log.debug("토큰 파싱 시도");
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            
            log.debug("토큰 파싱 성공, 소유자 확인: user_id={}, email={}", 
                    claims.getBody().get("user_id"), 
                    claims.getBody().get("email"));
            
            Date expiration = claims.getBody().getExpiration();
            Date now = new Date();
            boolean isValid = !expiration.before(now);
            
            if (!isValid) {
                log.warn("토큰이 만료되었습니다. expiration={}, now={}", expiration, now);
            } else {
                log.debug("토큰이 유효합니다. expiration={}, now={}", expiration, now);
            }
            
            return isValid;
        } catch (Exception e) {
            log.warn("토큰 검증 오류: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 토큰의 만료 시간을 초 단위로 반환합니다.
     * 양수일 경우 현재 시간부터 만료까지 남은 초, 음수일 경우 이미 만료됨을 의미합니다.
     */
    public long getExpirationTime(String token) {
        try {
            // 토큰에서 만료 시간 추출
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            
            // 현재 시간과의 차이 계산 (초 단위)
            Date now = new Date();
            return (expiration.getTime() - now.getTime()) / 1000;
        } catch (Exception e) {
            // 토큰 파싱 오류 또는 만료된 토큰
            return -1;
        }
    }
    
    /**
     * 커스텀 만료 시간을 가진 JWT 토큰 생성 (개발용)
     */
    public String createTokenWithCustomExpiry(Long userId, String email, List<String> roles, Date expiryDate) {
        Claims claims = Jwts.claims();
        claims.put("user_id", userId);
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("environment", "dev"); // 개발용 토큰 표시
        
        Date now = new Date();
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
