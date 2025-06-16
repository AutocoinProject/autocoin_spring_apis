package com.autocoin.global.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 보안 감사 AOP
 * 
 * - 민감한 작업에 대한 보안 로깅
 * - 사용자 행동 추적
 * - 의심스러운 활동 탐지
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditAspect {

    // 감사 대상 메서드 패턴
    private static final List<String> AUDIT_METHODS = Arrays.asList(
            "login", "logout", "signup", "changePassword", "updateProfile",
            "connect", "disconnect", "sync", "transfer", "withdraw",
            "create", "update", "delete", "admin"
    );

    // 민감한 작업 패턴
    private static final List<String> SENSITIVE_METHODS = Arrays.asList(
            "connect", "disconnect", "transfer", "withdraw", "admin"
    );

    /**
     * 인증 관련 메서드 감사
     */
    @Around("execution(* com.autocoin.user.api.*Controller.*(..))")
    public Object auditAuthOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "AUTH");
    }

    /**
     * 업비트 관련 메서드 감사
     */
    @Around("execution(* com.autocoin.upbit.api.*Controller.*(..))")
    public Object auditUpbitOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "UPBIT");
    }

    /**
     * 거래 관련 메서드 감사
     */
    @Around("execution(* com.autocoin.trading.api.*Controller.*(..))")
    public Object auditTradingOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "TRADING");
    }

    /**
     * 관리자 작업 감사
     */
    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditAdminOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "ADMIN");
    }

    /**
     * 보안 작업 감사 공통 로직
     */
    private Object auditSecurityOperation(ProceedingJoinPoint joinPoint, String operationType) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        
        // 감사 대상인지 확인
        if (!isAuditTarget(methodName)) {
            return joinPoint.proceed();
        }
        
        // 요청 정보 수집
        SecurityAuditInfo auditInfo = collectSecurityInfo(joinPoint, operationType);
        
        // 사전 로깅
        logSecurityOperationStart(auditInfo);
        
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
            
            // 사후 로깅
            logSecurityOperationEnd(auditInfo, result, exception, duration);
        }
    }

    /**
     * 감사 대상 확인
     */
    private boolean isAuditTarget(String methodName) {
        return AUDIT_METHODS.stream()
                .anyMatch(pattern -> methodName.toLowerCase().contains(pattern.toLowerCase()));
    }

    /**
     * 보안 정보 수집
     */
    private SecurityAuditInfo collectSecurityInfo(ProceedingJoinPoint joinPoint, String operationType) {
        SecurityAuditInfo auditInfo = new SecurityAuditInfo();
        
        // 메서드 정보
        auditInfo.operationType = operationType;
        auditInfo.className = joinPoint.getTarget().getClass().getSimpleName();
        auditInfo.methodName = joinPoint.getSignature().getName();
        auditInfo.fullMethodName = auditInfo.className + "." + auditInfo.methodName;
        
        // 사용자 정보
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            auditInfo.userId = authentication.getName();
            auditInfo.authorities = authentication.getAuthorities().toString();
            auditInfo.isAuthenticated = true;
        } else {
            auditInfo.userId = "anonymous";
            auditInfo.authorities = "none";
            auditInfo.isAuthenticated = false;
        }
        
        // HTTP 요청 정보
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditInfo.clientIp = getClientIpAddress(request);
            auditInfo.userAgent = request.getHeader("User-Agent");
            auditInfo.sessionId = request.getSession(false) != null ? 
                    request.getSession().getId() : "no-session";
        }
        
        // 민감도 결정
        auditInfo.isSensitive = SENSITIVE_METHODS.stream()
                .anyMatch(pattern -> auditInfo.methodName.toLowerCase().contains(pattern.toLowerCase()));
        
        return auditInfo;
    }

    /**
     * 보안 작업 시작 로깅
     */
    private void logSecurityOperationStart(SecurityAuditInfo auditInfo) {
        if (auditInfo.isSensitive) {
            // 민감한 작업은 WARN 레벨로 로깅
            log.warn("🔐 [SECURITY AUDIT] 🚨 {} 민감한 작업 시작 - User: {}, Method: {}, IP: {}, Session: {}", 
                    auditInfo.operationType,
                    auditInfo.userId,
                    auditInfo.fullMethodName,
                    auditInfo.clientIp,
                    auditInfo.sessionId);
        } else {
            // 일반 작업은 INFO 레벨로 로깅
            log.info("🔐 [SECURITY AUDIT] {} 작업 시작 - User: {}, Method: {}, IP: {}", 
                    auditInfo.operationType,
                    auditInfo.userId,
                    auditInfo.fullMethodName,
                    auditInfo.clientIp);
        }
        
        // 익명 사용자의 민감한 작업 시도
        if (!auditInfo.isAuthenticated && auditInfo.isSensitive) {
            log.error("🔐 [SECURITY ALERT] 🚨 익명 사용자의 민감한 작업 시도! Method: {}, IP: {}", 
                    auditInfo.fullMethodName, auditInfo.clientIp);
        }
    }

    /**
     * 보안 작업 완료 로깅
     */
    private void logSecurityOperationEnd(SecurityAuditInfo auditInfo, Object result, Throwable exception, long duration) {
        if (exception != null) {
            // 실패한 보안 작업
            log.error("🔐 [SECURITY AUDIT] ❌ {} 작업 실패 - User: {}, Method: {}, Duration: {}ms, Error: {}, IP: {}", 
                    auditInfo.operationType,
                    auditInfo.userId,
                    auditInfo.fullMethodName,
                    duration,
                    exception.getClass().getSimpleName(),
                    auditInfo.clientIp);
            
            // 반복적인 실패 감지 (향후 구현 가능)
            detectSuspiciousActivity(auditInfo, exception);
            
        } else {
            // 성공한 보안 작업
            if (auditInfo.isSensitive) {
                log.warn("🔐 [SECURITY AUDIT] ✅ {} 민감한 작업 완료 - User: {}, Method: {}, Duration: {}ms, IP: {}", 
                        auditInfo.operationType,
                        auditInfo.userId,
                        auditInfo.fullMethodName,
                        duration,
                        auditInfo.clientIp);
            } else {
                log.info("🔐 [SECURITY AUDIT] ✅ {} 작업 완료 - User: {}, Method: {}, Duration: {}ms", 
                        auditInfo.operationType,
                        auditInfo.userId,
                        auditInfo.fullMethodName,
                        duration);
            }
        }
    }

    /**
     * 의심스러운 활동 탐지
     */
    private void detectSuspiciousActivity(SecurityAuditInfo auditInfo, Throwable exception) {
        // 인증 실패 패턴
        if (exception.getMessage() != null && 
            (exception.getMessage().contains("Authentication") || 
             exception.getMessage().contains("Unauthorized") ||
             exception.getMessage().contains("Access Denied"))) {
            
            log.error("🔐 [SECURITY ALERT] 🚨 의심스러운 인증 시도 - User: {}, IP: {}, Error: {}", 
                    auditInfo.userId, auditInfo.clientIp, exception.getMessage());
        }
        
        // 향후 추가할 수 있는 탐지 패턴:
        // - 동일 IP에서 반복적인 실패
        // - 비정상적인 시간대의 접근
        // - 권한 상승 시도
        // - 대량 데이터 접근 시도
    }

    /**
     * 클라이언트 IP 주소 추출
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
     * 보안 감사 정보 클래스
     */
    private static class SecurityAuditInfo {
        String operationType;
        String className;
        String methodName;
        String fullMethodName;
        String userId;
        String authorities;
        boolean isAuthenticated;
        String clientIp;
        String userAgent;
        String sessionId;
        boolean isSensitive;
    }
}
