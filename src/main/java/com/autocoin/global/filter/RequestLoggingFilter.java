package com.autocoin.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 모든 HTTP 요청과 응답을 로깅하는 필터
 * 실무에서는 비활성화 - ApiLoggingFilter 사용
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@ConditionalOnProperty(
    prefix = "autocoin.logging.request-filter", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 요청 로깅
            logRequest(requestWrapper);
            
            // 다음 필터 실행
            chain.doFilter(requestWrapper, responseWrapper);
            
            // 응답 로깅
            logResponse(responseWrapper, System.currentTimeMillis() - startTime);
        } finally {
            // 응답 내용을 다시 복사하여 클라이언트에게 전송
            responseWrapper.copyBodyToResponse();
        }
    }
    
    private void logRequest(HttpServletRequest request) {
        // 요청 시작 로그
        String queryString = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String requestUrl = request.getRequestURL().toString() + queryString;
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("API 요청: {} {}", request.getMethod(), requestUrl);
        log.info("클라이언트 IP: {}", getClientIp(request));
        log.info("User-Agent: {}", request.getHeader("User-Agent"));
        
        // 요청 헤더 로깅
        Map<String, String> headerMap = getHeaderMap(request);
        if (!headerMap.isEmpty()) {
            log.info("요청 헤더:");
            headerMap.forEach((key, value) -> 
                log.info("    {} : {}", key, key.toLowerCase().contains("authorization") ? "********" : value));
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        int status = response.getStatus();
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("API 응답: HTTP 상태 {}", status);
        log.info("응답 시간: {} ms", durationMs);
        
        // 응답 헤더 로깅
        Collection<String> headerNames = response.getHeaderNames();
        if (!headerNames.isEmpty()) {
            log.info("응답 헤더:");
            for (String headerName : headerNames) {
                String headerValue = response.getHeader(headerName);
                log.info("    {} : {}", headerName, headerName.toLowerCase().contains("authorization") ? "********" : headerValue);
            }
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, request.getHeader(headerName));
        }
        
        return headerMap;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_CLIENT_IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        
        return clientIp;
    }
}
