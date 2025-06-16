package com.autocoin.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Mock Slack 알림 서비스 구현체
 * 
 * <p>테스트나 개발 환경에서 사용되는 Slack 알림 서비스의 모크 구현체입니다.</p>
 * <p>실제 Slack Webhook을 호출하지 않고 콘솔 로그로만 출력합니다.</p>
 * 
 * <h3>사용 조건:</h3>
 * <ul>
 *   <li><code>slack.notifications.enabled=false</code> 또는 설정이 없는 경우</li>
 *   <li>개발 환경에서 Slack 연동 없이 테스트하고 싶을 때</li>
 *   <li>CI/CD 파이프라인에서 자동 테스트 실행 시</li>
 * </ul>
 * 
 * <h3>장점:</h3>
 * <ul>
 *   <li>Slack Webhook URL 설정 없이도 애플리케이션 실행 가능</li>
 *   <li>로그를 통해 알림 발생 상황 모니터링 가능</li>
 *   <li>네트워크 연결 없이도 알림 로직 테스트 가능</li>
 * </ul>
 * 
 * @author AutoCoin Team
 * @version 1.0
 * @since 1.0
 * @see RealSlackNotificationService
 * @see SlackNotificationService
 */

@Service
@Slf4j
@ConditionalOnProperty(name = "slack.notifications.enabled", havingValue = "false", matchIfMissing = true)
public class MockSlackNotificationService implements SlackNotificationService {

    /**
     * 기본 메시지 전송 모크 구현
     * 
     * <p>실제 Slack으로 전송하지 않고 INFO 레벨로 로그를 출력합니다.</p>
     * 
     * @param message 로그로 출력할 메시지
     */
    @Override
    public void sendMessage(String message) {
        log.info("🤖 Mock Slack 메시지: {}", message);
    }
    
    /**
     * 채널별 메시지 전송 모크 구현
     * 
     * <p>채널 정보와 메시지를 함께 로그로 출력합니다.</p>
     * 
     * @param channel 명시될 채널명
     * @param message 로그로 출력할 메시지
     */
    @Override
    public void sendMessage(String channel, String message) {
        log.info("🤖 Mock Slack 메시지: [{}] {}", channel, message);
    }
    
    /**
     * 에러 메시지 전송 모크 구현
     * 
     * <p>에러 내용과 예외 정보를 로그로 출력합니다.</p>
     * 
     * @param error 에러 메시지
     * @param throwable 예외 객체 (nullable)
     */
    @Override
    public void sendErrorMessage(String error, Throwable throwable) {
        log.info("🎆 Mock Slack 에러 메시지: {} - {}", error, 
                throwable != null ? throwable.getMessage() : "예외 없음");
    }

    /**
     * 에러 알림 전송 모크 구현
     * 
     * <p>에러 알림 내용을 구조화된 형태로 로그로 출력합니다.</p>
     * 
     * @param title 에러 알림 제목
     * @param message 에러 메시지 내용
     * @param exception 예외 객체 (nullable)
     */
    @Override
    public void sendErrorNotification(String title, String message, Exception exception) {
        log.info("😨 Mock Slack 에러 알림: [{}] {} - {}", title, message, 
                exception != null ? exception.getMessage() : "예외 없음");
    }

    /**
     * 정보 알림 전송 모크 구현
     * 
     * <p>정보성 알림을 로그로 출력합니다.</p>
     * 
     * @param title 정보 알림 제목
     * @param message 정보 메시지 내용
     */
    @Override
    public void sendInfoNotification(String title, String message) {
        log.info("ℹ️ Mock Slack 정보 알림: [{}] {}", title, message);
    }

    /**
     * 성공 알림 전송 모크 구현
     * 
     * <p>성공 알림을 로그로 출력합니다.</p>
     * 
     * @param title 성공 알림 제목
     * @param message 성공 메시지 내용
     */
    @Override
    public void sendSuccessNotification(String title, String message) {
        log.info("✅ Mock Slack 성공 알림: [{}] {}", title, message);
    }

    /**
     * 경고 알림 전송 모크 구현
     * 
     * <p>경고 알림을 로그로 출력합니다.</p>
     * 
     * @param title 경고 알림 제목
     * @param message 경고 메시지 내용
     */
    @Override
    public void sendWarningNotification(String title, String message) {
        log.info("⚠️ Mock Slack 경고 알림: [{}] {}", title, message);
    }

    /**
     * 거래 알림 전송 모크 구현
     * 
     * <p>거래 정보를 로그로 출력합니다.</p>
     * <p>실제 구현체에서와 달리 간단한 형태로 출력됩니다.</p>
     * 
     * @param market 거래 마켓
     * @param type 거래 타입
     * @param price 거래 가격
     * @param amount 거래 수량
     */
    @Override
    public void sendTradeNotification(String market, String type, String price, String amount) {
        log.info("💰 Mock Slack 거래 알림: {} {} - 가격: {}, 수량: {}", market, type, price, amount);
    }

    /**
     * 애플리케이션 시작 알림 모크 구현
     * 
     * <p>애플리케이션 시작을 로그로 알림합니다.</p>
     */
    @Override
    public void sendStartupNotification() {
        log.info("🚀 Mock Slack 시작 알림: AutoCoin 애플리케이션이 시작되었습니다.");
    }

    /**
     * 애플리케이션 종료 알림 모크 구현
     * 
     * <p>애플리케이션 종료를 로그로 알림합니다.</p>
     */
    @Override
    public void sendShutdownNotification() {
        log.info("🛑 Mock Slack 종료 알림: AutoCoin 애플리케이션이 종료됩니다.");
    }

    /**
     * 헬스체크 알림 모크 구현
     * 
     * <p>헬스체크 결과를 로그로 출력합니다.</p>
     * 
     * @param status 헬스체크 상태
     * @param details 상세 정보
     */
    @Override
    public void sendHealthCheckNotification(String status, Map<String, Object> details) {
        log.info("🏥 Mock Slack 헬스체크 알림: 상태={}, 세부사항={}", status, details);
    }
}
