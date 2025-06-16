package com.autocoin.chart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket 설정 클래스
 * - STOMP 프로토콜 사용
 * - SimpleBroker 사용 (인메모리)
 * - CORS 설정 포함
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 엔드포인트 등록
     * - 클라이언트가 /ws 경로로 WebSocket 연결
     * - 직접 WebSocket 연결 지원 (개발용)
     * - CORS 허용
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 직접 WebSocket 연결 (개발용)
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .addInterceptors(new HttpSessionHandshakeInterceptor()); // 핸드셰이크 인터셉터 추가
        
        // SockJS 폴백 지원 (프로덕션용)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins("http://localhost:3000")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS()
                .setHeartbeatTime(25000) // 하트비트 설정
                .setDisconnectDelay(5000); // 연결 끊기 대기 시간
        
        log.info("STOMP WebSocket endpoint registered at /ws and /ws-sockjs");
    }

    /**
     * 메시지 브로커 설정
     * - /topic 으로 시작하는 주제에 대해 SimpleBroker 사용
     * - 클라이언트가 /app 으로 메시지 전송 시 컨트롤러로 라우팅
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // SimpleBroker 활성화 - 인메모리 메시지 브로커
        registry.enableSimpleBroker("/topic");
        
        // 클라이언트에서 서버로 메시지 전송 시 prefix
        registry.setApplicationDestinationPrefixes("/app");
        
        log.info("Message broker configured - Topic: /topic, App prefix: /app");
    }
}
