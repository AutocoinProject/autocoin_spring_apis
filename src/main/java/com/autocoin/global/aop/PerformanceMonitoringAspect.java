package com.autocoin.global.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 성능 모니터링 AOP
 * 
 * - API 메서드 실행 시간 측정
 * - 느린 메서드 감지 및 로깅
 * - Prometheus 메트릭 수집
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringAspect {

    private final MeterRegistry meterRegistry;
    
    // 성능 임계값 (밀리초)
    private static final long SLOW_METHOD_THRESHOLD = 1000L; // 1초
    private static final long VERY_SLOW_METHOD_THRESHOLD = 3000L; // 3초

    /**
     * Controller 메서드 성능 모니터링
     */
    @Around("execution(* com.autocoin.*.api.*Controller.*(..))")
    public Object monitorControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethodPerformance(joinPoint, "controller");
    }

    /**
     * Service 메서드 성능 모니터링
     */
    @Around("execution(* com.autocoin.*.application.*Service.*(..))")
    public Object monitorServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethodPerformance(joinPoint, "service");
    }

    /**
     * Repository 메서드 성능 모니터링
     */
    @Around("execution(* com.autocoin.*.infrastructure.*Repository.*(..))")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethodPerformance(joinPoint, "repository");
    }

    /**
     * 외부 API 호출 성능 모니터링
     */
    @Around("execution(* com.autocoin.*.infrastructure.*Client.*(..))")
    public Object monitorExternalApiPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethodPerformance(joinPoint, "external-api");
    }

    /**
     * 공통 성능 모니터링 로직
     */
    private Object monitorMethodPerformance(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        // StopWatch로 실행 시간 측정
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // Micrometer Timer로 메트릭 수집
        Timer.Sample sample = Timer.start(meterRegistry);
        
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Micrometer 메트릭 기록
            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("layer", layer)
                    .tag("status", exception != null ? "error" : "success")
                    .register(meterRegistry));
            
            // 로깅 처리
            logPerformance(fullMethodName, executionTime, exception, layer);
            
            // 성능 카운터 증가
            incrementPerformanceCounters(layer, executionTime, exception != null);
        }
    }

    /**
     * 성능 로깅 처리
     */
    private void logPerformance(String methodName, long executionTime, Throwable exception, String layer) {
        if (exception != null) {
            // 에러 발생 시 항상 로깅
            log.error("[{}] 🚨 {} 실행 실패 - {}ms, Error: {}", 
                    layer.toUpperCase(), methodName, executionTime, exception.getMessage());
        } else if (executionTime >= VERY_SLOW_METHOD_THRESHOLD) {
            // 매우 느린 메서드 (3초 이상)
            log.warn("[{}] 🐌 {} 매우 느린 실행 - {}ms", 
                    layer.toUpperCase(), methodName, executionTime);
        } else if (executionTime >= SLOW_METHOD_THRESHOLD) {
            // 느린 메서드 (1초 이상)
            log.warn("[{}] ⚠️ {} 느린 실행 - {}ms", 
                    layer.toUpperCase(), methodName, executionTime);
        } else {
            // 정상 실행 (DEBUG 레벨)
            log.debug("[{}] ✅ {} 실행 완료 - {}ms", 
                    layer.toUpperCase(), methodName, executionTime);
        }
    }

    /**
     * 성능 카운터 증가
     */
    private void incrementPerformanceCounters(String layer, long executionTime, boolean hasError) {
        // 총 실행 횟수
        meterRegistry.counter("method.execution.count",
                "layer", layer,
                "status", hasError ? "error" : "success")
                .increment();
        
        // 느린 메서드 카운터
        if (executionTime >= SLOW_METHOD_THRESHOLD) {
            meterRegistry.counter("method.execution.slow",
                    "layer", layer,
                    "threshold", executionTime >= VERY_SLOW_METHOD_THRESHOLD ? "very_slow" : "slow")
                    .increment();
        }
        
        // 에러 카운터
        if (hasError) {
            meterRegistry.counter("method.execution.error",
                    "layer", layer)
                    .increment();
        }
    }
}
