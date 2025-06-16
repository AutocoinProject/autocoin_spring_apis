package com.autocoin.upbit.infrastructure;

import com.autocoin.upbit.application.UpbitAuthService;
import com.autocoin.upbit.dto.UpbitAccountInfoDto;
import com.autocoin.upbit.dto.UpbitTickerDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitApiClient {
    
    private final RestTemplate restTemplate;
    private final UpbitAuthService upbitAuthService;
    private final ObjectMapper objectMapper;
    
    @Value("${app.upbit.api.url}")
    private String upbitApiUrl;
    
    /**
     * 계정 정보 조회
     */
    public List<UpbitAccountInfoDto> getAccounts(String accessKey, String secretKey) {
        try {
            String url = upbitApiUrl + "/v1/accounts";
            String authToken = upbitAuthService.generateAuthorizationToken(accessKey, secretKey, null);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Accept", "application/json");
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return objectMapper.readValue(response.getBody(), new TypeReference<List<UpbitAccountInfoDto>>() {});
            
        } catch (Exception e) {
            log.error("계정 정보 조회 API 호출 실패", e);
            throw new RuntimeException("계정 정보 조회 실패", e);
        }
    }
    
    /**
     * 마켓 코드 조회
     */
    public List<Map<String, Object>> getMarkets() {
        try {
            String url = upbitApiUrl + "/v1/market/all";
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
            
        } catch (Exception e) {
            log.error("마켓 코드 조회 API 호출 실패", e);
            throw new RuntimeException("마켓 코드 조회 실패", e);
        }
    }
    
    /**
     * 시세 정보 조회
     */
    public List<UpbitTickerDto> getTickers(List<String> markets) {
        try {
            String marketsParam = markets.stream()
                    .map(market -> URLEncoder.encode(market, StandardCharsets.UTF_8))
                    .collect(Collectors.joining(","));
            
            String url = UriComponentsBuilder.fromHttpUrl(upbitApiUrl + "/v1/ticker")
                    .queryParam("markets", marketsParam)
                    .toUriString();
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<UpbitTickerDto>>() {});
            
        } catch (Exception e) {
            log.error("시세 정보 조회 API 호출 실패", e);
            throw new RuntimeException("시세 정보 조회 실패", e);
        }
    }
    
    /**
     * 호가 정보 조회
     */
    public List<Map<String, Object>> getOrderbook(List<String> markets) {
        try {
            String marketsParam = markets.stream()
                    .map(market -> URLEncoder.encode(market, StandardCharsets.UTF_8))
                    .collect(Collectors.joining(","));
            
            String url = UriComponentsBuilder.fromHttpUrl(upbitApiUrl + "/v1/orderbook")
                    .queryParam("markets", marketsParam)
                    .toUriString();
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
            
        } catch (Exception e) {
            log.error("호가 정보 조회 API 호출 실패", e);
            throw new RuntimeException("호가 정보 조회 실패", e);
        }
    }
    
    /**
     * 캔들 조회 (일봉)
     */
    public List<Map<String, Object>> getDayCandles(String market, String to, int count) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(upbitApiUrl + "/v1/candles/days")
                    .queryParam("market", market)
                    .queryParam("count", count);
            
            if (to != null && !to.isEmpty()) {
                builder.queryParam("to", to);
            }
            
            String url = builder.toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
            
        } catch (Exception e) {
            log.error("일봉 조회 API 호출 실패", e);
            throw new RuntimeException("일봉 조회 실패", e);
        }
    }
    
    /**
     * 분봉 조회
     */
    public List<Map<String, Object>> getMinuteCandles(int unit, String market, String to, int count) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(upbitApiUrl + "/v1/candles/minutes/" + unit)
                    .queryParam("market", market)
                    .queryParam("count", count);
            
            if (to != null && !to.isEmpty()) {
                builder.queryParam("to", to);
            }
            
            String url = builder.toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
            
        } catch (Exception e) {
            log.error("분봉 조회 API 호출 실패", e);
            throw new RuntimeException("분봉 조회 실패", e);
        }
    }
    
    /**
     * 체결 내역 조회
     */
    public List<Map<String, Object>> getTrades(String market, String to, int count, String cursor) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(upbitApiUrl + "/v1/trades/ticks")
                    .queryParam("market", market)
                    .queryParam("count", count);
            
            if (to != null && !to.isEmpty()) {
                builder.queryParam("to", to);
            }
            if (cursor != null && !cursor.isEmpty()) {
                builder.queryParam("cursor", cursor);
            }
            
            String url = builder.toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
            
        } catch (Exception e) {
            log.error("체결 내역 조회 API 호출 실패", e);
            throw new RuntimeException("체결 내역 조회 실패", e);
        }
    }
}