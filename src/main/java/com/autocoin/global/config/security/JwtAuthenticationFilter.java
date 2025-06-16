package com.autocoin.global.config.security;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.autocoin.global.exception.core.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT 인증 필터
 * 
 * <p>Spring Security의 필터 체인에 통합되어 HTTP 요청마다 JWT 토큰 검증을 수행하는 필터입니다.</p>
 * <p>OncePerRequestFilter를 상속받아 요청당 한 번만 실행되도록 보장합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>요청 경로별 필터 적용 여부 결정</li>
 *   <li>JWT 토큰 추출 및 검증</li>
 *   <li>인증 성공 시 SecurityContext에 인증 정보 설정</li>
 *   <li>인증 실패 시 401 응답 반환</li>
 *   <li>상세한 로깅을 통한 디버깅 지원</li>
 * </ul>
 * 
 * <h3>필터 적용 전략:</h3>
 * <ul>
 *   <li><strong>제외 경로</strong>: 인증이 불필요한 공개 API</li>
 *   <li><strong>인증 필요 경로</strong>: 보호된 리소스에 대한 접근</li>
 *   <li><strong>조건부 인증</strong>: 특정 HTTP 메서드에만 인증 적용</li>
 * </ul>
 * 
 * @author AutoCoin Team
 * @version 1.0
 * @since 1.0
 * @see JwtTokenProvider
 * @see OncePerRequestFilter
 */

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 토큰 처리를 담당하는 서비스
     * 
     * <p>토큰 생성, 검증, 파싱 및 인증 정보 추출을 담당합니다.</p>
     */
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * JSON 직렬화/역직렬화를 위한 ObjectMapper
     * 
     * <p>에러 응답을 JSON 형태로 직렬화하여 클라이언트에 전송하기 위해 사용됩니다.</p>
     */
    private final ObjectMapper objectMapper;
    
    /**
     * JWT 필터에서 제외할 경로 목록
     * 
     * <p>이 경로들은 인증 없이도 접근할 수 있는 공개 API들입니다.</p>
     * <p>startsWith 비교를 사용하므로 하위 경로도 모두 제외됩니다.</p>
     * 
     * <h4>제외 경로 리스트:</h4>
     * <ul>
     *   <li><strong>/health, /api/health</strong>: 헬스체크 API</li>
     *   <li><strong>/api/cors-test</strong>: CORS 테스트 API</li>
     *   <li><strong>/api/v1/auth/login</strong>: 로그인 API</li>
     *   <li><strong>/api/v1/auth/signup</strong>: 회원가입 API</li>
     *   <li><strong>/api/v1/auth/refresh</strong>: 토큰 리프레시 API</li>
     *   <li><strong>/api/chart/</strong>: 차트 데이터 API</li>
     *   <li><strong>/oauth2/authorization/</strong>: OAuth2 로그인</li>
     *   <li><strong>/swagger-ui, /swagger-dev</strong>: API 문서</li>
     *   <li><strong>/v3/api-docs</strong>: OpenAPI 명세</li>
     * </ul>
     */
    
    private static final String[] EXCLUDED_PATHS = {
        "/health", 
        "/api/health",
        "/api/cors-test",
        "/api/v1/auth/login",     // 로그인만 제외
        "/api/v1/auth/signup",    // 회원가입만 제외
        "/api/v1/auth/refresh",   // 토큰 리프레시만 제외
        "/api/chart/",            // 차트 API 제외
        "/api/v1/chart/",         // 차트 API v1 제외
        "/oauth2/authorization/",
        "/swagger-ui",
        "/swagger-dev",
        "/v3/api-docs"
    };
    
    /**
     * 필터 적용 여부를 결정하는 메서드
     * 
     * <p>OncePerRequestFilter의 shouldNotFilter 메서드를 오버라이드하여 특정 경로에 대해 필터를 건너뛰도록 합니다.</p>
     * <p>반환값이 true이면 이 필터를 건너뛰고, false이면 필터를 실행합니다.</p>
     * 
     * <h4>필터 제외 로직:</h4>
     * <ol>
     *   <li><strong>정확한 경로 매칭</strong>: EXACT_EXCLUDED_PATHS에 있는 경로와 정확히 일치</li>
     *   <li><strong>접두사 매칭</strong>: EXCLUDED_PATHS에 있는 경로로 시작하는 모든 경로</li>
     * </ol>
     * 
     * <h4>예시:</h4>
     * <ul>
     *   <li>"/health" → 제외 (접두사 매칭)</li>
     *   <li>"/api/v1/auth/login" → 제외 (접두사 매칭)</li>
     *   <li>"/" → 제외 (정확한 매칭)</li>
     *   <li>"/api/v1/auth/me" → 필터 실행</li>
     * </ul>
     * 
     * @param request HTTP 요청 객체
     * @return true면 필터 건너뛰기, false면 필터 실행
     * @throws ServletException 서블릿 예외 발생 시
     */

    /**
     * 정확한 경로 매칭으로 제외할 경로 목록
     * 
     * <p>startsWith가 아닌 equals 비교를 사용하여 정확한 경로만 제외합니다.</p>
     * <p>루트 경로("/")와 같이 다른 경로와 충돌할 수 있는 경로에 사용됩니다.</p>
     */
    private static final String[] EXACT_EXCLUDED_PATHS = {
        "/"  // 루트 경로는 정확히 일치하는 경우만 제외
    };
    
    /**
     * 인증이 필요한 경로 목록
     * 
     * <p>이 경로들은 반드시 인증된 사용자만 접근할 수 있는 보호된 리소스들입니다.</p>
     * <p>JWT 토큰이 유효하지 않으면 401 에러를 반환합니다.</p>
     * 
     * <h4>인증 필요 경로 리스트:</h4>
     * <ul>
     *   <li><strong>/api/v1/posts</strong>: 게시글 관리 API</li>
     *   <li><strong>/api/v1/categories/init</strong>: 카테고리 초기화 (관리자만)</li>
     *   <li><strong>/api/v1/auth/me</strong>: 내 정보 조회</li>
     *   <li><strong>/api/v1/auth/logout</strong>: 로그아웃</li>
     *   <li><strong>/api/v1/upbit/**</strong>: 업비트 연동 API (개인 거래 정보)</li>
     * </ul>
     */
    private static final String[] AUTHENTICATED_PATHS = {
        "/api/v1/posts",
        "/api/v1/categories/init",  // 카테고리 초기화는 인증 필요
        "/api/v1/auth/me",          // 내 정보 조회는 인증 필요
        "/api/v1/auth/logout",       // 로그아웃도 인증 필요
        "/api/v1/upbit/"            // 업비트 API는 인증 필요
    };
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        log.info("[JWT 필터 체크] 경로 확인: {}", path);
        
        // 정확한 경로 매칭으로 제외 처리
        for (String exactPath : EXACT_EXCLUDED_PATHS) {
            if (path.equals(exactPath)) {
                log.info("[JWT 필터 체크] 정확 경로 매칭으로 제외: {} (이유: {})", path, exactPath);
                return true;
            }
        }
        
        // startsWith로 제외 처리
        for (String excludedPath : EXCLUDED_PATHS) {
            log.info("[JWT 필터 체크] 비교: '{}' startsWith('{}')", path, excludedPath);
            if (path.startsWith(excludedPath)) {
                log.info("[JWT 필터 체크] 필터 제외 경로로 건너뛰기: {} (이유: {})", path, excludedPath);
                return true;
            }
        }
        
        log.info("[JWT 필터 체크] 필터 실행 결정: {}", path);
        return false;
    }

    /**
     * JWT 인증 필터의 메인 실행 메서드
     * 
     * <p>요청마다 JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정하는 핵심 로직입니다.</p>
     * <p>OncePerRequestFilter의 doFilterInternal 메서드를 구현하여 요청당 한 번만 실행되도록 보장합니다.</p>
     * 
     * <h4>주요 처리 단계:</h4>
     * <ol>
     *   <li><strong>OPTIONS 요청 처리</strong>: CORS preflight 요청은 인증 없이 통과</li>
     *   <li><strong>인증 헤더 확인</strong>: Authorization 및 authorization-token 헤더 검사</li>
     *   <li><strong>JWT 토큰 추출</strong>: 헤더에서 Bearer 토큰 추출</li>
     *   <li><strong>인증 필요 경로 판단</strong>: 현재 경로가 인증이 필요한지 확인</li>
     *   <li><strong>토큰 검증</strong>: JWT 서명 및 만료 시간 검증</li>
     *   <li><strong>인증 정보 설정</strong>: 성공 시 SecurityContext에 Authentication 객체 설정</li>
     *   <li><strong>에러 처리</strong>: 실패 시 401 에러 응답 반환</li>
     * </ol>
     * 
     * <h4>인증 필요 경로 판단 로직:</h4>
     * <ul>
     *   <li><strong>특정 엔드포인트</strong>: POST /api/v1/categories, PUT/DELETE /api/v1/categories/*</li>
     *   <li><strong>일반 인증 경로</strong>: AUTHENTICATED_PATHS 배열에 정의된 경로</li>
     * </ul>
     * 
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체 
     * @param filterChain 필터 체인 객체
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException I/O 예외 발생 시
     */

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 요청 기본 정보 로깅
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();
        
        log.info("[JWT 필터 시작] {} {}, ContentType: {}", method, requestURI, contentType);
        
        // Skip filter for OPTIONS requests
        if (request.getMethod().equals("OPTIONS")) {
            log.info("[JWT 필터] OPTIONS 요청으로 필터 건너뛰기: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 인증 헤더 확인 (두 가지 헤더 모두 지원)
        String authHeader = request.getHeader("Authorization");
        String authToken = request.getHeader("authorization-token");
        
        log.info("[JWT 필터] 인증 헤더 확인: Authorization={}, authorization-token={}", 
                authHeader != null ? "존재(길이:" + authHeader.length() + ")" : "없음", 
                authToken != null ? "존재(길이:" + authToken.length() + ")" : "없음");
        
        // Extract JWT token from headers
        String token = jwtTokenProvider.resolveToken(request);
        
        // 토큰 여부 로깅
        if (token != null) {
            log.info("[JWT 필터] JWT 토큰 추출 성공: {}...", token.substring(0, Math.min(token.length(), 20)));
        } else {
            log.warn("[JWT 필터] JWT 토큰 추출 실패: {}", requestURI);
        }
        
        // 인증이 필요한 경로인지 확인
        boolean isAuthenticatedPath = false;
        
        // POST /api/v1/categories (카테고리 생성)
        if (requestURI.equals("/api/v1/categories") && "POST".equals(method)) {
            isAuthenticatedPath = true;
            log.info("[JWT 필터] 인증 필요 경로 확인: {} {} (카테고리 생성)", method, requestURI);
        }
        // PUT /api/v1/categories/{id} (카테고리 수정)
        else if (requestURI.startsWith("/api/v1/categories/") && "PUT".equals(method)) {
            isAuthenticatedPath = true;
            log.info("[JWT 필터] 인증 필요 경로 확인: {} {} (카테고리 수정)", method, requestURI);
        }
        // DELETE /api/v1/categories/{id} (카테고리 삭제)
        else if (requestURI.startsWith("/api/v1/categories/") && "DELETE".equals(method)) {
            isAuthenticatedPath = true;
            log.info("[JWT 필터] 인증 필요 경로 확인: {} {} (카테고리 삭제)", method, requestURI);
        }
        // 기존 인증 필요 경로들
        else {
            for (String authPath : AUTHENTICATED_PATHS) {
                if (requestURI.startsWith(authPath)) {
                    isAuthenticatedPath = true;
                    log.info("[JWT 필터] 인증 필요 경로 확인: {} {} (기존 설정)", method, requestURI);
                    break;
                }
            }
        }
        
        if (!isAuthenticatedPath) {
            log.info("[JWT 필터] 인증 불필요 경로, 통과: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 인증이 필요한 경로이지만 토큰이 없는 경우
        if (token == null) {
            log.error("[JWT 필터] 🔒 인증 필요하지만 토큰 없음: {}", requestURI);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .code("C001")
                    .message("인증에 실패했습니다. 로그인이 필요합니다.")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
            return;
        }
        
        try {
            // Validate token and set authentication if valid
            if (jwtTokenProvider.validateToken(token)) {
                log.info("[JWT 필터] ✅ 토큰 검증 성공: {}...", token.substring(0, Math.min(token.length(), 20)));
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                
                // 인증 정보 디버깅
                if (auth != null && auth.isAuthenticated()) {
                    Object principal = auth.getPrincipal();
                    log.info("[JWT 필터] ✅ 인증 성공: principal={}", 
                            principal != null ? principal.getClass().getSimpleName() : "null");
                    
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("[JWT 필터] ✅ SecurityContext에 인증 정보 설정 완료");
                } else {
                    log.warn("[JWT 필터] ❌ 인증 객체가 null이거나 인증되지 않음");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    ErrorResponse errorResponse = ErrorResponse.builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .code("C001")
                            .message("인증에 실패했습니다.")
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    objectMapper.writeValue(response.getOutputStream(), errorResponse);
                    return;
                }
            } else {
                log.warn("[JWT 필터] ❌ 토큰 검증 실패: {}...", token.substring(0, Math.min(token.length(), 20)));
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .code("C001")
                        .message("잘못된 토큰입니다.")
                        .timestamp(LocalDateTime.now())
                        .build();
                
                objectMapper.writeValue(response.getOutputStream(), errorResponse);
                return;
            }
            
            log.info("[JWT 필터] ✅ 필터 처리 완료, 다음 필터로 이동: {}", requestURI);
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            log.error("[JWT 필터] ❌ JWT 토큰 검증 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            
            // Send unauthorized response
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .code("C001")
                    .message("잘못된 토큰입니다.")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        } catch (Exception e) {
            log.error("[JWT 필터] ❌ JWT 토큰 처리 중 예외 발생: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }
}
