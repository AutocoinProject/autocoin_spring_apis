package com.autocoin.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "slack.notifications.enabled", havingValue = "true")
@Slf4j
public class RealSlackNotificationService implements SlackNotificationService {

    private RestTemplate restTemplate;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public RealSlackNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendMessage(String message) {
        sendSlackMessage("#general", message, null, null);
    }
    
    @Override
    public void sendMessage(String channel, String message) {
        sendSlackMessage(channel, message, null, null);
    }
    
    @Override
    public void sendErrorMessage(String error, Throwable throwable) {
        String fullMessage = String.format("🚨 *Error:* %s", error);
        
        if (throwable != null) {
            fullMessage += String.format("\n\n*Exception:* `%s`", throwable.getClass().getSimpleName());
            if (throwable.getMessage() != null) {
                fullMessage += String.format("\n*Message:* %s", throwable.getMessage());
            }
        }
        
        fullMessage += String.format("\n\n*Time:* %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#errors", fullMessage, "danger", "Error Alert");
    }

    @Override
    public void sendErrorNotification(String title, String message, Exception exception) {
        String fullMessage = String.format("🚨 *%s*\\n\\n```%s```", title, message);
        
        if (exception != null) {
            fullMessage += String.format("\\n\\n*Exception:* `%s`", exception.getClass().getSimpleName());
            if (exception.getMessage() != null) {
                fullMessage += String.format("\\n*Message:* %s", exception.getMessage());
            }
        }
        
        fullMessage += String.format("\\n\\n*Time:* %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#errors", fullMessage, "danger", "Application Error");
    }

    @Override
    public void sendInfoNotification(String title, String message) {
        String fullMessage = String.format("ℹ️ *%s*\\n\\n%s\\n\\n*Time:* %s", 
            title, 
            message,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#alerts", fullMessage, "good", "Information");
    }

    @Override
    public void sendSuccessNotification(String title, String message) {
        String fullMessage = String.format("✅ *%s*\\n\\n%s\\n\\n*Time:* %s", 
            title, 
            message,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#alerts", fullMessage, "good", "Success");
    }

    @Override
    public void sendWarningNotification(String title, String message) {
        String fullMessage = String.format("⚠️ *%s*\\n\\n%s\\n\\n*Time:* %s", 
            title, 
            message,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#alerts", fullMessage, "warning", "Warning");
    }

    @Override
    public void sendTradeNotification(String market, String type, String price, String amount) {
        try {
            // 이모지와 색상 설정
            String emoji = type.equalsIgnoreCase("BUY") ? "💰" : "💸";
            String typeEmoji = type.equalsIgnoreCase("BUY") ? "📈" : "📉";
            String color = type.equalsIgnoreCase("BUY") ? "good" : "#ff9500"; // 매수: 녹색, 매도: 주황색
            
            // 숫자 포맷팅
            double priceValue = Double.parseDouble(price);
            double amountValue = Double.parseDouble(amount);
            double totalValue = priceValue * amountValue;
            
            // 가격 포맷팅 (천 단위 콤마)
            String formattedPrice = String.format("%,.0f", priceValue);
            String formattedTotal = String.format("%,.0f", totalValue);
            
            // 코인 심볼 추출 (KRW-BTC -> BTC)
            String coinSymbol = market.contains("-") ? market.split("-")[1] : market;
            
            // 메시지 구성
            String message = String.format("%s **%s %s 거래 완료** %s\n\n", emoji, market, type.toUpperCase(), typeEmoji);
            message += String.format("💎 **코인:** %s\n", coinSymbol);
            message += String.format("💰 **단가:** %s 원\n", formattedPrice);
            message += String.format("📊 **수량:** %s %s\n", amount, coinSymbol);
            message += String.format("💵 **총 거래금액:** %s 원\n", formattedTotal);
            message += String.format("🕐 **거래시간:** %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            sendSlackMessage("#trades", message, color, String.format("%s %s Trade Alert", typeEmoji, market));
            
        } catch (NumberFormatException e) {
            log.error("거래 알림 전송 중 숫자 포맷 오류: price={}, amount={}, error={}", price, amount, e.getMessage());
            // 기본 메시지로 폴백
            String fallbackMessage = String.format("%s **%s %s**\n\n**Price:** %s\n**Amount:** %s\n**Time:** %s", 
                type.equalsIgnoreCase("BUY") ? "📈" : "📉", 
                type.toUpperCase(), 
                market, 
                price, 
                amount,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sendSlackMessage("#trades", fallbackMessage, "warning", String.format("%s Trade Alert", market));
        } catch (Exception e) {
            log.error("거래 알림 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void sendStartupNotification() {
        String message = "🚀 *AutoCoin Application Started*\\n\\n";
        message += String.format("*Environment:* %s\\n", getEnvironment());
        message += String.format("*Start Time:* %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#alerts", message, "good", "Application Startup");
    }

    @Override
    public void sendShutdownNotification() {
        String message = "🛑 *AutoCoin Application Shutdown*\\n\\n";
        message += String.format("*Environment:* %s\\n", getEnvironment());
        message += String.format("*Shutdown Time:* %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#alerts", message, "warning", "Application Shutdown");
    }

    @Override
    public void sendHealthCheckNotification(String status, Map<String, Object> details) {
        String emoji = "UP".equals(status) ? "✅" : "❌";
        String color = "UP".equals(status) ? "good" : "danger";
        
        String message = String.format("%s *Health Check: %s*\\n\\n", emoji, status);
        
        if (details != null && !details.isEmpty()) {
            message += "*Details:*\\n";
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                message += String.format("• %s: %s\\n", entry.getKey(), entry.getValue());
            }
        }
        
        message += String.format("\\n*Time:* %s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        sendSlackMessage("#alerts", message, color, "Health Check");
    }

    private void sendSlackMessage(String channel, String message, String color, String title) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Slack Webhook URL이 설정되지 않았습니다.");
            return;
        }
        
        try {
            Map<String, Object> slackMessage = new HashMap<>();
            slackMessage.put("channel", channel);
            slackMessage.put("username", "AutoCoin Bot");
            slackMessage.put("icon_emoji", ":robot_face:");

            if (title != null || color != null) {
                Map<String, Object> attachment = new HashMap<>();
                if (title != null) attachment.put("title", title);
                if (color != null) attachment.put("color", color);
                attachment.put("text", message);
                attachment.put("ts", System.currentTimeMillis() / 1000);
                
                slackMessage.put("attachments", new Object[]{attachment});
            } else {
                slackMessage.put("text", message);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(slackMessage, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.debug("Slack 메시지 전송 성공: {}", channel);

        } catch (Exception e) {
            log.error("Slack 메시지 전송 실패: {}", e.getMessage());
        }
    }

    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "unknown");
    }
}
