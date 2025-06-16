package com.autocoin.global.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * API 호출 로깅 AOP
 * 
 * - HTTP 요청/응답 로깅
 * - 사용자 행동 추적
 * - 보안 감사 로그
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiLoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * Controller 메서드 호출 로깅
     */
    @Around("execution(* com.autocoin.*.api.*Controller.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        // 요청 정보 수집
        ApiCallInfo callInfo = collectRequestInfo(request, joinPoint);
        
        // 요청 로깅
        logRequest(callInfo);
        
        Object result = null;
        Throwable exception = null;
        long startTime = System.currentTimeMillis();
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 응답 로깅
            logResponse(callInfo, result, exception, duration);
        }
    }

    /**
     * 요청 정보 수집
     */
    private ApiCallInfo collectRequestInfo(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        ApiCallInfo callInfo = new ApiCallInfo();
        
        // HTTP 정보
        callInfo.method = request.getMethod();
        callInfo.uri = request.getRequestURI();
        callInfo.queryString = request.getQueryString();
        callInfo.clientIp = getClientIpAddress(request);
        callInfo.userAgent = request.getHeader("User-Agent");
        
        // 인증 정보 (민감 정보 제외)
        String authHeader = request.getHeader("Authorization");
        callInfo.isAuthenticated = authHeader != null && authHeader.startsWith("Bearer ");
        
        // 메서드 정보
        callInfo.className = joinPoint.getTarget().getClass().getSimpleName();
        callInfo.methodName = joinPoint.getSignature().getName();
        
        // 파라미터 정보 (민감 정보 마스킹)
        callInfo.parameters = maskSensitiveData(joinPoint.getArgs());
        
        // 요청 헤더 (민감 정보 제외)
        callInfo.headers = collectSafeHeaders(request);
        
        return callInfo;
    }

    /**
     * 요청 로깅
     */
    private void logRequest(ApiCallInfo callInfo) {
        try {
            log.info("🔍 [API REQUEST] {} {} - IP: {}, Class: {}, Method: {}, Auth: {}", 
                    callInfo.method, 
                    callInfo.uri + (callInfo.queryString != null ? "?" + callInfo.queryString : ""),
                    callInfo.clientIp,
                    callInfo.className,
                    callInfo.methodName,
                    callInfo.isAuthenticated ? "✅" : "❌");
            
            // 상세 로그 (DEBUG 레벨)
            if (log.isDebugEnabled()) {
                log.debug("📋 [API REQUEST DETAIL] Headers: {}, Params: {}, UserAgent: {}", 
                        callInfo.headers, 
                        callInfo.parameters, 
                        callInfo.userAgent);
            }
        } catch (Exception e) {
            log.warn("API 요청 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 응답 로깅
     */
    private void logResponse(ApiCallInfo callInfo, Object result, Throwable exception, long duration) {
        try {
            if (exception != null) {
                // 에러 응답
                log.error("🚨 [API ERROR] {} {} - {}ms, Error: {}, IP: {}", 
                        callInfo.method, 
                        callInfo.uri,
                        duration,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                        callInfo.clientIp);
            } else {
                // 정상 응답
                String responseInfo = result != null ? result.getClass().getSimpleName() : "void";
                
                if (duration > 1000) {
                    // 느린 응답 (1초 이상)
                    log.warn("🐌 [API SLOW] {} {} - {}ms, Response: {}, IP: {}", 
                            callInfo.method, 
                            callInfo.uri,
                            duration,
                            responseInfo,
                            callInfo.clientIp);
                } else {
                    // 정상 응답
                    log.info("✅ [API SUCCESS] {} {} - {}ms, Response: {}, IP: {}", 
                            callInfo.method, 
                            callInfo.uri,
                            duration,
                            responseInfo,
                            callInfo.clientIp);
                }
            }
        } catch (Exception e) {
            log.warn("API 응답 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 민감한 데이터 마스킹
     */
    private Object[] maskSensitiveData(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return null;
                    }
                    
                    String argString = arg.toString();
                    String className = arg.getClass().getSimpleName();
                    
                    // 비밀번호, 토큰 등 민감 정보 마스킹
                    if (argString.toLowerCase().contains("password") ||
                        argString.toLowerCase().contains("token") ||
                        argString.toLowerCase().contains("secret") ||
                        argString.toLowerCase().contains("key")) {
                        return className + "(***MASKED***)";
                    }
                    
                    // 긴 문자열은 요약
                    if (argString.length() > 200) {
                        return className + "(" + argString.substring(0, 200) + "...truncated)";
                    }
                    
                    return className + "(" + argString + ")";
                })
                .toArray();
    }

    /**
     * 안전한 헤더만 수집
     */
    private Map<String, String> collectSafeHeaders(HttpServletRequest request) {
        Map<String, String> safeHeaders = new HashMap<>();
        
        // 안전한 헤더 목록
        String[] safeHeaderNames = {
                "Content-Type", "Accept", "Accept-Language", 
                "Accept-Encoding", "Origin", "Referer"
        };
        
        for (String headerName : safeHeaderNames) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                safeHeaders.put(headerName, headerValue);
            }
        }
        
        return safeHeaders;
    }

    /**
     * API 호출 정보 클래스
     */
    private static class ApiCallInfo {
        String method;
        String uri;
        String queryString;
        String clientIp;
        String userAgent;
        boolean isAuthenticated;
        String className;
        String methodName;
        Object[] parameters;
        Map<String, String> headers;
    }
}
