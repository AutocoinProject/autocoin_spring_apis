package com.autocoin.global.aop;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 모니터링 AOP
 * 
 * - 데이터베이스 트랜잭션 모니터링
 * - 트랜잭션 성능 측정
 * - 롱 트랜잭션 감지
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionMonitoringAspect {

    private final MeterRegistry meterRegistry;
    
    // 롱 트랜잭션 임계값 (밀리초)
    private static final long LONG_TRANSACTION_THRESHOLD = 5000L; // 5초
    private static final long VERY_LONG_TRANSACTION_THRESHOLD = 10000L; // 10초

    /**
     * @Transactional 메서드 모니터링
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        // 트랜잭션 상태 확인
        boolean isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean isNewTransaction = !TransactionSynchronizationManager.isSynchronizationActive() || 
                                  TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        log.debug("🔄 [TRANSACTION START] {} - Active: {}, New: {}", 
                fullMethodName, isTransactionActive, isNewTransaction);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 트랜잭션 종료 로깅
            logTransactionEnd(fullMethodName, duration, exception, isTransactionActive);
            
            // 메트릭 수집
            recordTransactionMetrics(className, methodName, duration, exception != null);
        }
    }

    /**
     * Repository 메서드 모니터링 (데이터베이스 접근)
     */
    @Around("execution(* com.autocoin.*.infrastructure.*Repository.*(..))")
    public Object monitorDatabaseAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 데이터베이스 접근 로깅
            logDatabaseAccess(fullMethodName, duration, exception);
            
            // 메트릭 수집
            recordDatabaseMetrics(className, methodName, duration, exception != null);
        }
    }

    /**
     * 트랜잭션 종료 로깅
     */
    private void logTransactionEnd(String methodName, long duration, Throwable exception, boolean wasTransactionActive) {
        if (exception != null) {
            // 트랜잭션 롤백
            log.error("🔄 [TRANSACTION ROLLBACK] {} - {}ms, Error: {}", 
                    methodName, duration, exception.getClass().getSimpleName());
        } else if (duration >= VERY_LONG_TRANSACTION_THRESHOLD) {
            // 매우 긴 트랜잭션
            log.warn("🔄 [TRANSACTION VERY_LONG] {} - {}ms ⚠️ 매우 긴 트랜잭션!", 
                    methodName, duration);
        } else if (duration >= LONG_TRANSACTION_THRESHOLD) {
            // 긴 트랜잭션
            log.warn("🔄 [TRANSACTION LONG] {} - {}ms ⚠️ 긴 트랜잭션", 
                    methodName, duration);
        } else {
            // 정상 트랜잭션
            log.debug("🔄 [TRANSACTION COMMIT] {} - {}ms", methodName, duration);
        }
    }

    /**
     * 데이터베이스 접근 로깅
     */
    private void logDatabaseAccess(String methodName, long duration, Throwable exception) {
        if (exception != null) {
            log.error("🗄️ [DB ERROR] {} - {}ms, Error: {}", 
                    methodName, duration, exception.getClass().getSimpleName());
        } else if (duration >= 1000) {
            // 느린 쿼리
            log.warn("🗄️ [DB SLOW] {} - {}ms 🐌", methodName, duration);
        } else {
            log.debug("🗄️ [DB ACCESS] {} - {}ms", methodName, duration);
        }
    }

    /**
     * 트랜잭션 메트릭 기록
     */
    private void recordTransactionMetrics(String className, String methodName, long duration, boolean hasError) {
        // 트랜잭션 실행 시간
        meterRegistry.timer("transaction.duration",
                "class", className,
                "method", methodName,
                "status", hasError ? "error" : "success")
                .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // 트랜잭션 카운터
        meterRegistry.counter("transaction.count",
                "class", className,
                "status", hasError ? "rollback" : "commit")
                .increment();
        
        // 긴 트랜잭션 카운터
        if (duration >= LONG_TRANSACTION_THRESHOLD) {
            meterRegistry.counter("transaction.long.count",
                    "class", className,
                    "severity", duration >= VERY_LONG_TRANSACTION_THRESHOLD ? "very_long" : "long")
                    .increment();
        }
    }

    /**
     * 데이터베이스 메트릭 기록
     */
    private void recordDatabaseMetrics(String className, String methodName, long duration, boolean hasError) {
        // 데이터베이스 접근 시간
        meterRegistry.timer("database.access.duration",
                "repository", className,
                "method", methodName,
                "status", hasError ? "error" : "success")
                .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // 데이터베이스 접근 카운터
        meterRegistry.counter("database.access.count",
                "repository", className,
                "status", hasError ? "error" : "success")
                .increment();
        
        // 느린 쿼리 카운터
        if (duration >= 1000) {
            meterRegistry.counter("database.slow.query.count",
                    "repository", className)
                    .increment();
        }
    }
}
