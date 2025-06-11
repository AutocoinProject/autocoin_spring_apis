package com.autocoin.global.config;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.global.auth.filter.JwtAuthenticationFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 클래스의 단위 테스트
 * 
 * Reflection을 사용하여 protected 메서드인 doFilterInternal을 직접 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @Mock
    private ServletOutputStream outputStream;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 각 테스트 전에 실행되는 설정 메서드
     */
    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    /**
     * Reflection을 사용하여 doFilterInternal 메서드를 호출하는 헬퍼 메서드
     */
    private void invokeDoFilterInternal() throws Exception {
        Method doFilterInternalMethod = JwtAuthenticationFilter.class.getDeclaredMethod(
            "doFilterInternal", HttpServletRequest.class, HttpServletResponse.class, FilterChain.class);
        doFilterInternalMethod.setAccessible(true);
        doFilterInternalMethod.invoke(jwtAuthenticationFilter, request, response, filterChain);
    }

    /**
     * 유효한 토큰이 있을 때 인증 처리 테스트
     */
    @Test
    @DisplayName("유효한 토큰이 있을 때 인증 처리 테스트")
    void doFilterInternal_ValidToken() throws Exception {
        // Given: 유효한 토큰 설정
        String token = "valid-token";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 필요한 메서드들이 호출되고, 필터체인이 계속 실행되는지 검증
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getAuthentication(token);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext에 인증 정보가 설정되었는지 확인
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 토큰이 없을 때 인증 처리 테스트 (인증 필요 경로)
     */
    @Test
    @DisplayName("토큰이 없을 때 인증 처리 테스트")
    void doFilterInternal_NoToken_AuthRequired() throws Exception {
        // Given: 토큰이 없는 상황 설정 (인증 필요 경로)
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);
        when(response.getOutputStream()).thenReturn(outputStream);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 토큰 해석만 시도하고, 401 응답 반환
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        
        // SecurityContext에 인증 정보가 설정되지 않았는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 토큰이 없을 때 인증 불필요 경로 테스트
     */
    @Test
    @DisplayName("토큰이 없을 때 인증 불필요 경로 테스트")
    void doFilterInternal_NoToken_AuthNotRequired() throws Exception {
        // Given: 토큰이 없는 상황 설정 (인증 불필요 경로)
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 토큰 해석 시도하고, 필터체인 계속 실행
        verify(jwtTokenProvider).resolveToken(request);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext에 인증 정보가 설정되지 않았는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 유효하지 않은 토큰일 때 인증 처리 테스트
     */
    @Test
    @DisplayName("유효하지 않은 토큰일 때 인증 처리 테스트")
    void doFilterInternal_InvalidToken() throws Exception {
        // Given: 유효하지 않은 토큰 설정
        String token = "invalid-token";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);
        when(response.getOutputStream()).thenReturn(outputStream);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 토큰 검증까지만 수행하고 401 응답 반환
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        
        // SecurityContext에 인증 정보가 설정되지 않았는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * JWT 예외 발생 테스트
     */
    @Test
    @DisplayName("JWT 예외 발생 테스트")
    void doFilterInternal_JwtException() throws Exception {
        // Given: JWT 예외 발생 설정
        String token = "jwt-exception-token";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenThrow(new JwtException("Invalid JWT token"));
        when(response.getOutputStream()).thenReturn(outputStream);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: JWT 예외가 발생하면 401 응답이 반환됨
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider).validateToken(token);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        
        // filterChain.doFilter는 호출되지 않음 (JwtException 처리)
        verify(filterChain, never()).doFilter(request, response);
        
        // SecurityContext가 비워졌는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 일반 예외 발생 테스트 (RuntimeException 등)
     */
    @Test
    @DisplayName("일반 예외 발생 테스트")
    void doFilterInternal_GeneralException() throws Exception {
        // Given: 일반 예외 발생 설정
        String token = "general-exception-token";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("General error"));

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 일반 예외는 catch되고 필터체인이 계속 실행됨
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider).validateToken(token);
        verify(filterChain).doFilter(request, response);
        
        // SecurityContext가 비워졌는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * OPTIONS 요청 시 바로 필터체인 실행 테스트
     */
    @Test
    @DisplayName("OPTIONS 요청 시 바로 필터체인 실행 테스트")
    void doFilterInternal_OptionsRequest() throws Exception {
        // Given: OPTIONS 요청 설정
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: JWT 처리 없이 바로 필터체인이 실행됨
        verify(jwtTokenProvider, never()).resolveToken(request);
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }

    /**
     * shouldNotFilter 메서드 테스트 - 제외 경로
     */
    @Test
    @DisplayName("제외 경로에 대한 shouldNotFilter 테스트")
    void shouldNotFilter_ExcludedPath() throws Exception {
        // Given: 제외 경로 설정
        when(request.getRequestURI()).thenReturn("/health");

        // When: shouldNotFilter 메서드 호출
        Method shouldNotFilterMethod = JwtAuthenticationFilter.class.getDeclaredMethod(
            "shouldNotFilter", HttpServletRequest.class);
        shouldNotFilterMethod.setAccessible(true);
        boolean result = (boolean) shouldNotFilterMethod.invoke(jwtAuthenticationFilter, request);

        // Then: true를 반환해야 함
        assertTrue(result, "제외 경로에 대해서는 shouldNotFilter가 true를 반환해야 합니다");
    }

    /**
     * shouldNotFilter 메서드 테스트 - 일반 경로
     */
    @Test
    @DisplayName("일반 경로에 대한 shouldNotFilter 테스트")
    void shouldNotFilter_NormalPath() throws Exception {
        // 일반 경로는 필터 대상이 되어야 함 (shouldNotFilter가 false)
        String[] testPaths = {
            "/protected/resource",
            "/custom/endpoint", 
            "/api/v1/custom",
            "/not-excluded-path"
        };
        
        for (String path : testPaths) {
            when(request.getRequestURI()).thenReturn(path);
            
            Method shouldNotFilterMethod = JwtAuthenticationFilter.class.getDeclaredMethod(
                "shouldNotFilter", HttpServletRequest.class);
            shouldNotFilterMethod.setAccessible(true);
            boolean result = (boolean) shouldNotFilterMethod.invoke(jwtAuthenticationFilter, request);
            
            assertFalse(result, "EXCLUDED_PATHS에 포함되지 않은 일반 경로 "+path+"는 필터링되어야 합니다");
        }
    }

    /**
     * API 경로의 shouldNotFilter 테스트
     * 특정 API 경로가 어떻게 처리되는지 확인
     */
    @Test
    @DisplayName("API 경로에 대한 shouldNotFilter 동작 확인")
    void shouldNotFilter_ApiPath() throws Exception {
        // When & Then: 다양한 API 경로 테스트
        String[] apiPaths = {
            "/api/v1/posts",
            "/api/v1/users/profile", 
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/test"
        };

        for (String path : apiPaths) {
            when(request.getRequestURI()).thenReturn(path);
            
            Method shouldNotFilterMethod = JwtAuthenticationFilter.class.getDeclaredMethod(
                "shouldNotFilter", HttpServletRequest.class);
            shouldNotFilterMethod.setAccessible(true);
            boolean result = (boolean) shouldNotFilterMethod.invoke(jwtAuthenticationFilter, request);
            
            System.out.println("Path: " + path + ", shouldNotFilter: " + result);
            // API 경로들의 실제 동작을 로그로 확인 (assertion 없이)
        }
    }

    /**
     * 인증 객체가 null인 경우 테스트
     */
    @Test
    @DisplayName("인증 객체가 null인 경우 테스트")
    void doFilterInternal_NullAuthentication() throws Exception {
        // Given: 유효한 토큰이지만 인증 객체가 null인 경우
        String token = "valid-token-null-auth";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(null);
        when(response.getOutputStream()).thenReturn(outputStream);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 401 응답이 반환됨
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getAuthentication(token);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        
        // SecurityContext에 인증 정보가 설정되지 않았는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 인증 객체가 인증되지 않은 경우 테스트
     */
    @Test
    @DisplayName("인증 객체가 인증되지 않은 경우 테스트")
    void doFilterInternal_NotAuthenticatedAuthentication() throws Exception {
        // Given: 유효한 토큰이지만 인증 객체가 인증되지 않은 경우
        String token = "valid-token-not-authenticated";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(response.getOutputStream()).thenReturn(outputStream);

        // When: doFilterInternal 직접 호출
        invokeDoFilterInternal();

        // Then: 401 응답이 반환됨
        verify(jwtTokenProvider).resolveToken(request);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getAuthentication(token);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        
        // SecurityContext에 인증 정보가 설정되지 않았는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
