package com.autocoin.trading.application;

import com.autocoin.trading.dto.TradingStartRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlaskTradingService {
    
    @Value("${FLASK_API_URL:http://localhost:5000}")
    private String flaskApiUrl;
    
    private final RestTemplate restTemplate;
    
    /**
     * 자동매매 시작 요청
     */
    public Map<String, Object> startTrading(String jwtToken, TradingStartRequestDto request) {
        try {
            String url = flaskApiUrl + "/api/trading/start";
            
            HttpHeaders headers = createHeaders(jwtToken);
            
            Map<String, Object> body = new HashMap<>();
            body.put("strategy", request.getStrategy());
            body.put("symbol", request.getSymbol());
            body.put("amount", request.getAmount());
            body.put("stop_loss", request.getStopLoss());
            body.put("take_profit", request.getTakeProfit());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            log.info("Flask API 자동매매 시작 요청: {}", url);
            log.debug("요청 데이터: {}", body);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            log.info("Flask API 응답: {} - {}", response.getStatusCode(), response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Flask API 호출 실패 - URL: {}, 오류: {}", flaskApiUrl, e.getMessage(), e);
            throw new RuntimeException("자동매매 시작 요청 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 자동매매 중지 요청
     */
    public Map<String, Object> stopTrading(String jwtToken) {
        try {
            String url = flaskApiUrl + "/api/trading/stop";
            
            HttpHeaders headers = createHeaders(jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Flask API 자동매매 중지 요청: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("자동매매 중지 요청 실패: {}", e.getMessage(), e);
            throw new RuntimeException("자동매매 중지 요청 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 자동매매 상태 조회
     */
    public Map<String, Object> getTradingStatus(String jwtToken) {
        try {
            String url = flaskApiUrl + "/api/trading/status";
            
            HttpHeaders headers = createHeaders(jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("자동매매 상태 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("자동매매 상태 조회 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * Flask API 헬스체크
     */
    public boolean isFlaskApiHealthy() {
        try {
            String url = flaskApiUrl + "/health";
            log.debug("Flask API 헬스체크 요청: {}", url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            
            log.info("Flask API 헬스체크 결과: {} - {}", 
                    isHealthy ? "정상" : "비정상", response.getBody());
            return isHealthy;
        } catch (Exception e) {
            log.warn("Flask API 헬스체크 실패 - URL: {}, 오류: {}", flaskApiUrl, e.getMessage());
            return false;
        }
    }
    
    /**
     * Flask API JWT 인증 테스트
     */
    public Map<String, Object> testFlaskAuth(String jwtToken) {
        try {
            String url = flaskApiUrl + "/health/auth-test";
            
            HttpHeaders headers = createHeaders(jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Flask API JWT 인증 테스트 요청: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            log.info("Flask API JWT 인증 테스트 성공: {} - {}", 
                    response.getStatusCode(), response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Flask API JWT 인증 테스트 실패 - URL: {}, 오류: {}", flaskApiUrl, e.getMessage(), e);
            throw new RuntimeException("Flask JWT 인증 테스트 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        return headers;
    }
}
