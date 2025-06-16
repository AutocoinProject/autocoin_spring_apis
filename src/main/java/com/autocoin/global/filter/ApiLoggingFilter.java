package com.autocoin.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 실무 수준 API 로깅 필터
 * - 에러 중심 로깅
 * - 중요한 비즈니스 API만 상세 로그
 * - 간소화된 성공 로그
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
@ConditionalOnProperty(
    prefix = "autocoin.logging.api-filter", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class ApiLoggingFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    
    // 로깅하지 않을 경로들
    private static final String[] EXCLUDED_PATHS = {
        "/actuator", "/health", "/favicon.ico", 
        "/swagger", "/v3/api-docs", "/webjars"
    };
    
    // 상세 로그가 필요한 중요 경로들
    private static final String[] IMPORTANT_PATHS = {
        "/api/v1/auth", "/api/v1/trading", "/api/v1/chart"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        
        // 제외 경로 체크
        if (shouldSkipLogging(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Trace ID 생성
        String traceId = generateTraceId();
        MDC.put(TRACE_ID, traceId);
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        
        long startTime = System.currentTimeMillis();
        
        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(requestWrapper, responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
            MDC.remove(TRACE_ID);
        }
    }
    
    private boolean shouldSkipLogging(String path) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isImportantPath(String path) {
        for (String importantPath : IMPORTANT_PATHS) {
            if (path.startsWith(importantPath)) {
                return true;
            }
        }
        return false;
    }
    
    private void logRequest(ContentCachingRequestWrapper request, 
                          ContentCachingResponseWrapper response, 
                          long duration) {
        
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();
        String clientIp = getClientIp(request);
        
        // 기본 로그 (간소화)
        if (status >= 400) {
            // 에러인 경우 상세 로그
            if (status >= 500) {
                log.error("API ERROR - {} {} - {} - {}ms - IP: {}", 
                    method, path, status, duration, clientIp);
            } else {
                log.warn("API WARN - {} {} - {} - {}ms - IP: {}", 
                    method, path, status, duration, clientIp);
            }
            
            // 중요한 API의 에러는 요청/응답 본문 포함
            if (isImportantPath(path)) {
                logDetailedError(request, response);
            }
        } else {
            // 성공인 경우 간단하게
            log.info("{} {} - {} - {}ms", method, path, status, duration);
        }
    }
    
    private void logDetailedError(ContentCachingRequestWrapper request, 
                                ContentCachingResponseWrapper response) {
        try {
            // 요청 본문 (민감 정보 마스킹)
            String requestBody = getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                requestBody = maskSensitiveData(requestBody);
                log.warn("Request Body: {}", requestBody);
            }
            
            // 응답 본문
            String responseBody = getResponseBody(response);
            if (responseBody != null && !responseBody.isEmpty()) {
                log.warn("Response Body: {}", responseBody);
            }
        } catch (Exception e) {
            log.debug("Failed to log request/response details", e);
        }
    }
    
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }
    
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }
    
    private String maskSensitiveData(String data) {
        if (data == null) return null;
        
        // 민감한 정보 마스킹
        return data
            .replaceAll("(\"password\"\\s*:\\s*\")[^\"]*\"", "$1\"******\"")
            .replaceAll("(\"token\"\\s*:\\s*\")[^\"]*\"", "$1\"******\"")
            .replaceAll("(\"secret\"\\s*:\\s*\")[^\"]*\"", "$1\"******\"")
            .replaceAll("Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*", "Bearer ******");
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
