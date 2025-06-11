package com.autocoin.notification.service;

import java.util.Map;

/**
 * Slack 알림 서비스 인터페이스
 * 
 * <p>AutoCoin 애플리케이션에서 발생하는 다양한 종류의 알림을 Slack으로 전송하기 위한 인터페이스입니다.</p>
 * <p>이 인터페이스는 두 가지 구현체를 가집니다:</p>
 * <ul>
 *   <li><strong>RealSlackNotificationService</strong>: 실제 Slack Webhook을 사용하여 메시지를 전송</li>
 *   <li><strong>MockSlackNotificationService</strong>: 테스트나 개발 환경에서 로깅만 수행</li>
 * </ul>
 * 
 * <h3>지원 기능:</h3>
 * <ul>
 *   <li>기본 메시지 전송</li>
 *   <li>채널별 메시지 전송</li>
 *   <li>시스템 알림 (성공/경고/에러/정보)</li>
 *   <li>거래 알림</li>
 *   <li>애플리케이션 라이프사이클 알림</li>
 *   <li>헬스체크 알림</li>
 * </ul>
 * 
 * @author AutoCoin Team
 * @version 1.0
 * @since 1.0
 * @see RealSlackNotificationService
 * @see MockSlackNotificationService
 */
public interface SlackNotificationService {
    
    /**
     * 기본 메시지를 Slack으로 전송
     * 
     * <p>기본 채널(#general)에 단순한 텍스트 메시지를 전송합니다.</p>
     * <p>빠르고 간단한 알림이 필요할 때 사용합니다.</p>
     * 
     * @param message 전송할 메시지 내용
     * @throws RuntimeException Slack 전송 실패 시 발생
     */
    void sendMessage(String message);
    
    /**
     * 지정된 채널에 메시지를 Slack으로 전송
     * 
     * <p>특정 Slack 채널에 메시지를 전송합니다.</p>
     * <p>채널명은 # 또는 @ 접두사를 사용할 수 있습니다. (예: #general, @username)</p>
     * 
     * @param channel 대상 채널명 (예: #trades, #alerts)
     * @param message 전송할 메시지 내용
     * @throws RuntimeException Slack 전송 실패 시 발생
     */
    void sendMessage(String channel, String message);
    
    /**
     * 에러 메시지를 Slack으로 전송
     * @param error 에러 메시지
     * @param throwable 예외 객체
     */
    void sendErrorMessage(String error, Throwable throwable);
    
    /**
     * 에러 알림 전송
     * @param title 제목
     * @param message 메시지
     * @param exception 예외 객체
     */
    void sendErrorNotification(String title, String message, Exception exception);
    
    /**
     * 정보 알림 전송
     * @param title 제목
     * @param message 메시지
     */
    void sendInfoNotification(String title, String message);
    
    /**
     * 성공 알림 전송
     * @param title 제목
     * @param message 메시지
     */
    void sendSuccessNotification(String title, String message);
    
    /**
     * 경고 알림 전송
     * @param title 제목
     * @param message 메시지
     */
    void sendWarningNotification(String title, String message);
    
    /**
     * 거래 알림 전송
     * @param market 마켓
     * @param type 거래 타입
     * @param price 가격
     * @param amount 수량
     */
    void sendTradeNotification(String market, String type, String price, String amount);
    
    /**
     * 애플리케이션 시작 알림
     */
    void sendStartupNotification();
    
    /**
     * 애플리케이션 종료 알림
     */
    void sendShutdownNotification();
    
    /**
     * 헬스체크 알림
     * 
     * <p>시스템의 헬스체크 결과를 Slack으로 전송합니다.</p>
     * <p>상태에 따라 다른 이모지와 색상이 사용됩니다:</p>
     * <ul>
     *   <li>UP: 녹색 배경, ✅ 이모지</li>
     *   <li>DOWN: 빨간색 배경, ❌ 이모지</li>
     * </ul>
     * 
     * @param status 헬스체크 상태 (UP, DOWN 등)
     * @param details 헬스체크 상세 정보 (각종 서비스 상태, 메모리 사용량 등)
     * @throws RuntimeException Slack 전송 실패 시 발생
     */
    void sendHealthCheckNotification(String status, Map<String, Object> details);
}
