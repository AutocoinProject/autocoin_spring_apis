package com.autocoin.global.config.aop;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 설정 클래스
 * 
 * @EnableAspectJAutoProxy: AspectJ Auto Proxy 활성화
 * - proxyTargetClass = true: CGLIB 프록시 사용 (클래스 기반)
 * - exposeProxy = true: 프록시 객체를 ThreadLocal에 노출
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class AopConfig {
    // AOP 설정만 담당하는 클래스
}
