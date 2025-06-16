package com.autocoin.trading.api;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.trading.application.FlaskTradingService;
import com.autocoin.trading.dto.TradingStartRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/trading")
@RequiredArgsConstructor
@Tag(name = "Trading", description = "자동매매 API")
public class TradingController {
    
    private final FlaskTradingService flaskTradingService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Operation(summary = "자동매매 시작", description = "Flask API를 통해 자동매매를 시작합니다")
    @PostMapping("/start")
    public ResponseEntity<?> startAutoTrading(
            @Valid @RequestBody TradingStartRequestDto request,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            String jwtToken = extractJwtFromRequest(httpRequest);
            if (jwtToken == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("JWT 토큰이 없습니다"));
            }
            
            log.info("사용자 {} 자동매매 시작 요청: {} {} {}", 
                authentication.getName(), request.getStrategy(), 
                request.getSymbol(), request.getAmount());
            
            Map<String, Object> result = flaskTradingService.startTrading(jwtToken, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "자동매매가 시작되었습니다.");
            response.put("data", result);
            response.put("user", authentication.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("자동매매 시작 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @Operation(summary = "자동매매 중지", description = "실행 중인 자동매매를 중지합니다")
    @PostMapping("/stop")
    public ResponseEntity<?> stopAutoTrading(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            String jwtToken = extractJwtFromRequest(httpRequest);
            if (jwtToken == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("JWT 토큰이 없습니다"));
            }
            
            log.info("사용자 {} 자동매매 중지 요청", authentication.getName());
            
            Map<String, Object> result = flaskTradingService.stopTrading(jwtToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "자동매매가 중지되었습니다.");
            response.put("data", result);
            response.put("user", authentication.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("자동매매 중지 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @Operation(summary = "자동매매 상태 조회", description = "현재 자동매매 실행 상태를 조회합니다")
    @GetMapping("/status")
    public ResponseEntity<?> getTradingStatus(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            String jwtToken = extractJwtFromRequest(httpRequest);
            if (jwtToken == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("JWT 토큰이 없습니다"));
            }
            
            Map<String, Object> result = flaskTradingService.getTradingStatus(jwtToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("user", authentication.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("자동매매 상태 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @Operation(summary = "Flask API 헬스체크", description = "Flask API 서버 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<?> checkFlaskHealth() {
        boolean isHealthy = flaskTradingService.isFlaskApiHealthy();
        
        Map<String, Object> response = new HashMap<>();
        response.put("flask_status", isHealthy ? "healthy" : "unhealthy");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Flask API 연결 테스트 (인증 필요)", description = "JWT 토큰으로 Flask API 연결을 테스트합니다")
    @GetMapping("/test-connection")
    public ResponseEntity<?> testFlaskConnection(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            String jwtToken = extractJwtFromRequest(httpRequest);
            if (jwtToken == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("JWT 토큰이 없습니다"));
            }
            
            log.info("사용자 {} Flask API 연결 테스트 요청", authentication.getName());
            
            // Flask API 헬스체크 호출
            boolean isHealthy = flaskTradingService.isFlaskApiHealthy();
            
            // 인증된 사용자 정보와 함께 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Flask API 연결 테스트 완료");
            response.put("flask_status", isHealthy ? "healthy" : "unhealthy");
            response.put("user", authentication.getName());
            response.put("jwt_token_valid", true);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("Flask API 연결 테스트 성공 - 사용자: {}, Flask 상태: {}", 
                authentication.getName(), isHealthy ? "정상" : "비정상");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Flask API 연결 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @Operation(summary = "Flask JWT 인증 테스트", description = "Spring에서 Flask로 JWT 토큰을 전달하여 인증 테스트")
    @GetMapping("/test-flask-auth")
    public ResponseEntity<?> testFlaskAuth(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            String jwtToken = extractJwtFromRequest(httpRequest);
            if (jwtToken == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("JWT 토큰이 없습니다"));
            }
            
            log.info("사용자 {} Flask JWT 인증 테스트 요청", authentication.getName());
            
            // Flask API JWT 인증 테스트 호출
            Map<String, Object> flaskResult = flaskTradingService.testFlaskAuth(jwtToken);
            
            // Spring + Flask 다 성공한 경우 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Spring → Flask JWT 인증 테스트 성공");
            response.put("spring_user", authentication.getName());
            response.put("flask_response", flaskResult);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("Flask JWT 인증 테스트 전체 성공 - Spring 사용자: {}, Flask 사용자: {}", 
                authentication.getName(), flaskResult.get("user_id"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Flask JWT 인증 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @Operation(summary = "거래 알림 수신", description = "Flask에서 보내는 거래 결과 알림을 받습니다")
    @PostMapping("/notify")
    public ResponseEntity<?> receiveTradeNotification(@RequestBody Map<String, Object> notification) {
        try {
            String userId = (String) notification.get("user_id");
            String sessionId = (String) notification.get("session_id");
            Map<String, Object> tradeResult = (Map<String, Object>) notification.get("trade_result");
            
            log.info("거래 알림 수신 - 사용자: {}, 세션: {}, 결과: {}", userId, sessionId, tradeResult);
            
            // TODO: 데이터베이스에 거래 기록 저장
            // TODO: 사용자에게 실시간 알림 전송 (WebSocket)
            
            return ResponseEntity.ok(Map.of("status", "received"));
            
        } catch (Exception e) {
            log.error("거래 알림 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
