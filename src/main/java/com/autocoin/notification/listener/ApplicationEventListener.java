package com.autocoin.notification.listener;

import com.autocoin.notification.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 라이프사이클 이벤트 리스너
 * 
 * <p>Spring Boot 애플리케이션의 주요 라이프사이클 이벤트를 감지하여 Slack으로 알림을 전송합니다.</p>
 * <p>AutoCoin 애플리케이션의 시작과 종료를 자동으로 모니터링합니다.</p>
 * 
 * <h3>감지하는 이벤트:</h3>
 * <ul>
 *   <li><strong>ApplicationReadyEvent</strong>: 애플리케이션이 완전히 시작되었을 때</li>
 *   <li><strong>ContextClosedEvent</strong>: 애플리케이션이 종료될 때</li>
 * </ul>
 * 
 * <h3>장점:</h3>
 * <ul>
 *   <li>애플리케이션 라이프사이클 자동 모니터링</li>
 *   <li>서비스 장애 상황 신속한 감지</li>
 *   <li>운영자에게 실시간 상태 알림</li>
 *   <li>배포 및 재시작 히스토리 추적</li>
 * </ul>
 * 
 * @author AutoCoin Team
 * @version 1.0
 * @since 1.0
 * @see SlackNotificationService
 * @see ApplicationReadyEvent
 * @see ContextClosedEvent
 */

@Component
@ConditionalOnBean(SlackNotificationService.class)
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventListener {

    /**
     * Slack 알림 서비스 의존성 주입
     * 
     * <p>SlackNotificationService의 구현체가 주입되며, 
     * 설정에 따라 MockSlackNotificationService 또는 RealSlackNotificationService가 사용됩니다.</p>
     */
    private final SlackNotificationService slackNotificationService;

    /**
     * 애플리케이션 시작 완료 이벤트 처리
     * 
     * <p>ApplicationReadyEvent를 감지하여 애플리케이션이 완전히 시작되었을 때 Slack 알림을 전송합니다.</p>
     * <p>이 이벤트는 모든 빈이 로드되고 애플리케이션이 요청을 받을 준비가 되었을 때 발생합니다.</p>
     * 
     * <h4>알림 내용:</h4>
     * <ul>
     *   <li>애플리케이션 시작 시간</li>
     *   <li>실행 환경 정보 (dev, prod 등)</li>
     *   <li>노초된 포트 정보</li>
     * </ul>
     * 
     * @param event ApplicationReadyEvent 객체
     */
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        log.info("애플리케이션 시작 완료 - Slack 알림 전송");
        slackNotificationService.sendStartupNotification();
    }

    /**
     * 애플리케이션 종료 이벤트 처리
     * 
     * <p>ContextClosedEvent를 감지하여 애플리케이션이 종료될 때 Slack 알림을 전송합니다.</p>
     * <p>이 이벤트는 정상 종료 또는 비정상 종료 시 모두 발생합니다.</p>
     * 
     * <h4>알림 내용:</h4>
     * <ul>
     *   <li>애플리케이션 종료 시간</li>
     *   <li>종료 이유 (정상/비정상)</li>
     *   <li>실행 환경 정보</li>
     * </ul>
     * 
     * <h4>주의사항:</h4>
     * <p>비정상 종료 시 이 알림이 전송되지 않을 수 있으므로 별도의 모니터링이 필요합니다.</p>
     * 
     * @param event ContextClosedEvent 객체
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("애플리케이션 종료 - Slack 알림 전송");
        slackNotificationService.sendShutdownNotification();
    }
}
