package com.autocoin.global.config;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JwtTokenProvider 단위 테스트
 * JWT 토큰 생성, 검증, 파싱 기능을 테스트
 */
@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private UserDetailsService userDetailsService;

    private JwtTokenProvider jwtTokenProvider;

    private final String secretKey = "test-jwt-secret-key-for-testing-purposes-only-not-for-production-environment-minimum-32-characters";
    private final long tokenValidTime = 30 * 60 * 1000L; // 30분
    private final Long userId = 1L;
    private final String email = "test@example.com";
    private final List<String> roles = Collections.singletonList("ROLE_USER");

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 인스턴스 생성
        jwtTokenProvider = new JwtTokenProvider(userDetailsService);
        
        // 비공개 필드 설정
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "tokenValidTime", tokenValidTime);
        
        // Key 객체 직접 설정
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtTokenProvider, "key", key);
    }

    @Test
    @DisplayName("JWT 토큰 생성 성공 테스트")
    void createToken_Success() {
        // when
        String token = jwtTokenProvider.createToken(userId, email, roles);

        // then
        assertNotNull(token, "토큰은 null이 아니어야 합니다");
        assertTrue(token.length() > 0, "토큰은 빈 문자열이 아니어야 합니다");
        
        // 토큰 구조 검증 (header.payload.signature)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT 토큰은 3개 부분으로 구성되어야 합니다");
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공 테스트")
    void getEmail_Success() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);

        // when
        String extractedEmail = jwtTokenProvider.getEmail(token);

        // then
        assertEquals(email, extractedEmail, "추출된 이메일은 원래 이메일과 일치해야 합니다");
    }

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 성공 테스트")
    void getUserId_Success() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);

        // when
        Long extractedId = jwtTokenProvider.getUserId(token);

        // then
        assertEquals(userId, extractedId, "추출된 사용자 ID는 원래 ID와 일치해야 합니다");
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공 테스트")
    void validateToken_ValidToken() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertTrue(isValid, "유효한 토큰은 검증을 통과해야 합니다");
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패 테스트")
    void validateToken_ExpiredToken() {
        // given - 만료된 토큰 직접 생성
        long now = System.currentTimeMillis();
        Date expirationDate = new Date(now - 1000); // 1초 전에 만료
        
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .claim("user_id", userId)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(new Date(now - 2000))
                .setExpiration(expirationDate)
                .signWith(key)
                .compact();

        // when
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // then
        assertFalse(isValid, "만료된 토큰은 유효하지 않아야 합니다");
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패 테스트")
    void validateToken_InvalidToken() {
        // given
        String invalidToken = "invalid.token.format";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertFalse(isValid, "잘못된 형식의 토큰은 유효하지 않아야 합니다");
    }

    @Test
    @DisplayName("토큰으로부터 인증 정보 조회 성공 테스트")
    void getAuthentication_Success() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);
        
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        given(userDetailsService.loadUserByUsername(anyString())).willReturn(userDetails);

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        assertNotNull(authentication, "인증 객체는 null이 아니어야 합니다");
        assertEquals(email, authentication.getName(), "인증 객체의 이름은 토큰의 이메일과 일치해야 합니다");
        assertTrue(authentication.isAuthenticated(), "인증 객체는 인증된 상태여야 합니다");
    }

    @Test
    @DisplayName("HTTP 요청에서 Bearer 토큰 추출 성공 테스트")
    void resolveToken_BearerToken_Success() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("authorization-token")).thenReturn(null);

        // when
        String resolvedToken = jwtTokenProvider.resolveToken(request);

        // then
        assertEquals(token, resolvedToken, "추출된 토큰은 원래 토큰과 일치해야 합니다");
    }

    @Test
    @DisplayName("HTTP 요청에서 authorization-token 헤더 추출 성공 테스트")
    void resolveToken_AuthorizationTokenHeader_Success() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("authorization-token")).thenReturn(token);

        // when
        String resolvedToken = jwtTokenProvider.resolveToken(request);

        // then
        assertEquals(token, resolvedToken, "authorization-token 헤더에서 추출된 토큰은 원래 토큰과 일치해야 합니다");
    }

    @Test
    @DisplayName("Authorization 헤더가 없는 경우 null 반환 테스트")
    void resolveToken_NoAuthorizationHeader() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("authorization-token")).thenReturn(null);

        // when
        String resolvedToken = jwtTokenProvider.resolveToken(request);

        // then
        assertNull(resolvedToken, "Authorization 헤더가 없는 경우 null을 반환해야 합니다");
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 토큰 처리 테스트")
    void resolveToken_NonBearerToken() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(token); // Bearer 없이
        when(request.getHeader("authorization-token")).thenReturn(null);

        // when
        String resolvedToken = jwtTokenProvider.resolveToken(request);

        // then
        assertEquals(token, resolvedToken, "Bearer 없는 토큰도 추출되어야 합니다");
    }

    @Test
    @DisplayName("토큰 만료 시간 확인 테스트")
    void getExpirationTime_ValidToken() {
        // given
        String token = jwtTokenProvider.createToken(userId, email, roles);

        // when
        long expirationTime = jwtTokenProvider.getExpirationTime(token);

        // then
        assertTrue(expirationTime > 0, "유효한 토큰의 만료 시간은 양수여야 합니다");
        assertTrue(expirationTime <= tokenValidTime / 1000, "만료 시간은 설정된 유효 시간 이하여야 합니다");
    }

    @Test
    @DisplayName("만료된 토큰의 만료 시간 확인 테스트")
    void getExpirationTime_ExpiredToken() {
        // given - 만료된 토큰 생성
        long now = System.currentTimeMillis();
        Date expirationDate = new Date(now - 1000);
        
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .claim("user_id", userId)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(new Date(now - 2000))
                .setExpiration(expirationDate)
                .signWith(key)
                .compact();

        // when
        long expirationTime = jwtTokenProvider.getExpirationTime(expiredToken);

        // then
        assertTrue(expirationTime < 0, "만료된 토큰의 만료 시간은 음수여야 합니다");
    }
}
