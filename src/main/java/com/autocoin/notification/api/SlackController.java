package com.autocoin.notification.api;

import com.autocoin.notification.service.SlackNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Slack 알림 REST API 컨트롤러
 * 
 * <p>이 컨트롤러는 Slack과의 통합을 위한 다양한 API 엔드포인트를 제공합니다.</p>
 * <p>Slack Webhook을 통해 메시지를 전송하고, 거래 알림, 시스템 알림 등을 관리합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>Slack 연동 상태 확인</li>
 *   <li>연결 테스트 메시지 전송</li>
 *   <li>사용자 정의 메시지 전송</li>
 *   <li>시스템 알림 (성공/경고/에러) 전송</li>
 *   <li>거래 완료 알림 전송</li>
 * </ul>
 * 
 * @author AutoCoin Team
 * @version 1.0
 * @since 1.0
 */

@RestController
@RequestMapping("/api/v1/slack")
@RequiredArgsConstructor
@ConditionalOnBean(SlackNotificationService.class)
@Tag(name = "Slack Notifications", description = "Slack 알림 API")
public class SlackController {

    /**
     * Slack 컨트롤러의 의존성 주입
     * 
     * <p>SlackNotificationService의 구현체가 주입되며, 
     * 설정에 따라 MockSlackNotificationService 또는 RealSlackNotificationService가 사용됩니다.</p>
     */
    private final SlackNotificationService slackNotificationService;

    /**
     * Slack 연동 상태 확인 API
     * 
     * <p>Slack Webhook 설정 상태와 서비스 가용성을 확인합니다.</p>
     * <p>Webhook URL이 설정되어 있고 서비스가 활성화되어 있는지 검사합니다.</p>
     * 
     * @return 연동 상태 정보가 포함된 ResponseEntity
     *         - webhook_configured: Webhook 설정 여부
     *         - service_available: 서비스 가용 여부
     *         - message: 상태 메시지
     */

    @GetMapping("/status")
    @Operation(summary = "Slack 연동 상태 확인", description = "Slack Webhook 설정 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> getSlackStatus() {
        return ResponseEntity.ok(Map.of(
            "webhook_configured", slackNotificationService != null,
            "service_available", slackNotificationService != null,
            "message", slackNotificationService != null ? "Slack 서비스가 활성화되어 있습니다." : "Slack 서비스가 비활성화되어 있습니다."
        ));
    }

    /**
     * Slack 연결 테스트 API
     * 
     * <p>Slack Webhook 연결을 테스트하기 위해 샘플 메시지를 전송합니다.</p>
     * <p>#general 채널에 성공 메시지를 보내어 Webhook이 제대로 작동하는지 확인합니다.</p>
     * 
     * @return 테스트 결과가 포함된 ResponseEntity
     *         - success: 테스트 성공 여부
     *         - message: 결과 메시지
     */

    @PostMapping("/test")
    @Operation(summary = "Slack 연결 테스트", description = "Slack Webhook 연결을 테스트합니다.")
    public ResponseEntity<Map<String, Object>> testSlackConnection() {
        try {
            slackNotificationService.sendMessage(
                "#general", 
                "✅ AutoCoin Slack 연동 테스트가 성공했습니다!"
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Slack 테스트 메시지가 전송되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Slack 연동에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 거래 완료 알림 전송 API
     * 
     * <p>암호화폐 거래가 완료되었을 때 Slack으로 상세한 거래 정보를 전송합니다.</p>
     * <p>다음 정보들이 포함된 알림이 전송됩니다:</p>
     * <ul>
     *   <li>거래 마켓 (예: KRW-BTC)</li>
     *   <li>거래 타입 (BUY/SELL)</li>
     *   <li>거래 단가 (천단위 콤마 포맷팅)</li>
     *   <li>거래 수량</li>
     *   <li>총 거래금액 (자동 계산)</li>
     *   <li>거래 시간</li>
     * </ul>
     * 
     * @param market 거래 마켓 (예: KRW-BTC, KRW-ETH)
     * @param type 거래 타입 (BUY 또는 SELL)
     * @param price 거래 단가 (원 단위)
     * @param amount 거래 수량 (코인 단위)
     * @return 전송 결과가 포함된 ResponseEntity
     *         - success: 전송 성공 여부
     *         - message: 결과 메시지
     *         - market: 거래된 마켓
     *         - type: 거래 타입
     */

    /**
     * 시스템 알림 메시지 전송 API
     * 
     * <p>시스템에서 발생하는 다양한 종류의 알림을 Slack으로 전송합니다.</p>
     * <p>지원되는 알림 타입:</p>
     * <ul>
     *   <li><strong>success</strong>: 성공 알림 (녹색, 체크 마크 이모지)</li>
     *   <li><strong>warning</strong>: 경고 알림 (노란색, 경고 이모지)</li>
     *   <li><strong>error</strong>: 에러 알림 (빨간색, 에러 이모지)</li>
     * </ul>
     * 
     * @param type 알림 타입 (success, warning, error 중 하나)
     * @param title 알림의 제목
     * @param message 알림의 상세 내용
     * @return 전송 결과가 포함된 ResponseEntity
     *         - success: 전송 성공 여부
     *         - message: 결과 메시지
     *         - type: 전송된 알림 타입
     */

    /**
     * 사용자 정의 메시지 전송 API
     * 
     * <p>지정된 Slack 채널에 사용자가 직접 입력한 메시지를 전송합니다.</p>
     * <p>채널명은 # 또는 @ 접두사를 사용할 수 있으며, 예시: #general, @username</p>
     * 
     * @param channel 메시지를 전송할 채널명 (예: #general)
     * @param message 전송할 메시지 내용
     * @return 전송 결과가 포함된 ResponseEntity
     *         - success: 전송 성공 여부
     *         - message: 결과 메시지
     *         - channel: 전송된 채널명
     */

    @PostMapping("/send")
    @Operation(summary = "사용자 정의 메시지 전송", description = "지정된 채널에 사용자 정의 메시지를 전송합니다.")
    public ResponseEntity<Map<String, Object>> sendCustomMessage(
            @Parameter(description = "채널명 (예: #general)", example = "#general")
            @RequestParam String channel,
            @Parameter(description = "전송할 메시지", example = "테스트 메시지입니다.")
            @RequestParam String message) {
        try {
            slackNotificationService.sendMessage(channel, message);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "메시지가 전송되었습니다.",
                "channel", channel
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "메시지 전송에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/alert")
    @Operation(summary = "알림 메시지 전송", description = "성공/경고/에러 알림을 전송합니다.")
    public ResponseEntity<Map<String, Object>> sendAlert(
            @Parameter(description = "알림 타입 (success, warning, error)", example = "success")
            @RequestParam String type,
            @Parameter(description = "알림 제목", example = "거래 성공")
            @RequestParam String title,
            @Parameter(description = "알림 내용", example = "BTC 매수 주문이 성공적으로 체결되었습니다.")
            @RequestParam String message) {
        try {
            switch (type.toLowerCase()) {
                case "success":
                    slackNotificationService.sendSuccessNotification(title, message);
                    break;
                case "warning":
                    slackNotificationService.sendWarningNotification(title, message);
                    break;
                case "error":
                    slackNotificationService.sendErrorNotification(title, message, null);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "잘못된 알림 타입입니다. (success, warning, error 중 선택)"
                    ));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", type.toUpperCase() + " 알림이 전송되었습니다.",
                "type", type
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "알림 전송에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/trade-alert")
    @Operation(summary = "거래 알림 전송", description = "거래 완료 알림을 전송합니다.")
    public ResponseEntity<Map<String, Object>> sendTradeAlert(
            @Parameter(description = "마켓 (예: KRW-BTC)", example = "KRW-BTC")
            @RequestParam String market,
            @Parameter(description = "거래 타입 (BUY, SELL)", example = "BUY")
            @RequestParam String type,
            @Parameter(description = "가격", example = "45000000")
            @RequestParam String price,
            @Parameter(description = "수량", example = "0.001")
            @RequestParam String amount) {
        try {
            slackNotificationService.sendTradeNotification(market, type, price, amount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "거래 알림이 전송되었습니다.",
                "market", market,
                "type", type
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "거래 알림 전송에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}
