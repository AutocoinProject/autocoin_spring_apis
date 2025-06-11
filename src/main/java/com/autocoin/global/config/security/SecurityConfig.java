package com.autocoin.global.config.security;

import com.autocoin.global.config.security.JwtAuthenticationFilter;
import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.global.exception.core.ErrorResponse;
import com.autocoin.user.oauth.CustomOAuth2UserService;
import com.autocoin.user.oauth.OAuth2AuthenticationSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    
    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;
    
    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    
    @Value("${oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:Authorization,Content-Type}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;
    
    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http
                // CSRF 보호 비활성화 (REST API에서는 일반적으로 비활성화)
                .csrf(AbstractHttpConfigurer::disable)
                
                // H2 콘솔 및 Swagger UI를 위한 헤더 설정
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())  // H2 콘솔의 iframe 허용
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig.disable())  // 개발환경에서 HTTPS 강제 비활성화
                )
                
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 세션 관리 설정: STATELESS로 설정해 세션을 사용하지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 인증 예외 처리 설정: 로그인 페이지로 리다이렉트 대신 JSON 응답 반환
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 인증 실패 로그 추가
                            String requestURI = request.getRequestURI();
                            String method = request.getMethod();
                            String userAgent = request.getHeader("User-Agent");
                            String authHeader = request.getHeader("Authorization");
                            
                            log.warn("인증 실패 - {} {} - UserAgent: {} - AuthHeader: {}", 
                                method, requestURI, userAgent, 
                                authHeader != null ? "Present" : "Missing");
                            
                            if (authException != null) {
                                log.debug("인증 예외 상세: {}", authException.getMessage());
                            }
                            
                            // 상태 코드 설정: 401 Unauthorized
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            // 응답 형식 설정: JSON
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            
                            // 오류 응답 생성
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
                            errorResponse.put("code", "C001");
                            errorResponse.put("message", "인증에 실패했습니다. 로그인이 필요합니다.");
                            errorResponse.put("timestamp", LocalDateTime.now().toString());
                            
                            // JSON 응답 작성
                            new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
                        })
                )
                
                // 요청 URL 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 API 엔드포인트
                        .requestMatchers("/", "/health", "/api/health").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/signup").permitAll()
                        .requestMatchers("/api/v1/auth/oauth2/**").permitAll()
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        // Swagger UI 및 OpenAPI 문서 관련 모든 경로 허용
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-ui/index.html").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        // Swagger 개발용 API (local 프로필에서만 동작)
                        .requestMatchers("/swagger-dev/**").permitAll()
                        // H2 Database Console (개발용만)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Spring Boot Actuator 엔드포인트들 - 보안 강화
                        .requestMatchers("/actuator/health").permitAll()  // 헬스체크만 공개
                        .requestMatchers("/actuator/info").permitAll()    // 기본 정보만 공개
                        .requestMatchers("/actuator/prometheus").permitAll()  // Prometheus 메트릭 공개
                        .requestMatchers("/actuator/**").hasAuthority("ADMIN")  // 나머지는 관리자만
                        // 테스트 API (개발용)
                        .requestMatchers("/api/v1/test/**").permitAll()
                        .requestMatchers("/api/v1/news/test/**").permitAll()
                        // 정적 리소스 (Swagger 자동 인증 스크립트 등)
                        .requestMatchers("/static/**").permitAll()
                        // Slack 테스트 API
                        .requestMatchers("/api/v1/slack/**").permitAll()
                        // Chart API - 차트 데이터는 인증 없이 접근 가능
                        .requestMatchers("/api/chart/**").permitAll()
                        .requestMatchers("/api/v1/chart/**").permitAll()
                        // 모니터링 API
                        .requestMatchers("/api/monitoring/**").permitAll()
                        // Trading API - 자동매매 기능
                        .requestMatchers("/api/v1/trading/**").authenticated()  // 자동매매 API는 인증 필요
                        .requestMatchers("/api/v1/trading/notify").permitAll()  // Flask 알림은 인증 없이 허용
                        // 카테고리 API - 조회는 공개, 생성/수정/삭제는 관리자만
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/categories/init").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasAuthority("ADMIN")
                        // 게시글 API - 인증 필요한 것들
                        .requestMatchers("/api/v1/posts").authenticated()  // 게시글 작성은 인증 필요
                        .requestMatchers("/api/v1/posts/my").authenticated() // 내 게시글 조회는 인증 필요
                        // 나머지 API는 인증 필요
                        .anyRequest().authenticated()
                );
                
        // OAuth2 로그인 설정 (조건부)
        if (oauth2Enabled && customOAuth2UserService != null && oAuth2AuthenticationSuccessHandler != null) {
            httpSecurity = httpSecurity.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    // OAuth2 로그인 페이지 경로 설정
                    .authorizationEndpoint(authorization -> authorization
                            .baseUri("/oauth2/authorization"))
            );
        }
                
        return httpSecurity
                // JWT 인증 필터 추가 - 모든 요청에 대해 JWT 토큰을 검증
                // UsernamePasswordAuthenticationFilter 이전에 실행되도록 설정
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, new ObjectMapper()), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 보안 강화: 허용할 오리진을 명시적으로 설정
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 멀티파트 요청을 처리하기 위한 리졸버 설정
     * - StandardServletMultipartResolver를 사용하여 파일 업로드 요청 처리
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    /**
     * 인증 관리자 빈 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Method Security Expression Handler 설정
     * @Secured("ADMIN") 형태로 사용하기 위해 ROLE_ prefix 제거
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setDefaultRolePrefix(""); // ROLE_ prefix 제거
        return handler;
    }
}
