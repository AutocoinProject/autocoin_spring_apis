package com.autocoin.chart.infrastructure;

import com.autocoin.chart.dto.UpbitCandleDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

/**
 * Upbit 차트 API 클라이언트
 * - 1분봉 캔들 데이터 조회
 * - 재시도 로직 포함
 * - Rate Limit 고려
 */
@Slf4j
@Component("chartUpbitApiClient")
@RequiredArgsConstructor
public class ChartUpbitApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.upbit.api.url:https://api.upbit.com}")
    private String upbitApiUrl;

    /**
     * 1분봉 캔들 데이터 조회
     * @param market 마켓 코드 (예: KRW-BTC)
     * @param count 조회할 캔들 개수 (최대 200)
     * @return 캔들 데이터 리스트 (최신순)
     */
    @Retryable(
        value = {Exception.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 1000)
    )
    public List<UpbitCandleDto> getCandles(String market, int count) {
        try {
            String url = String.format("%s/v1/candles/minutes/1?market=%s&count=%d", 
                                     upbitApiUrl, market, count);
            
            log.debug("Requesting Upbit candles: {}", url);
            
            String response = restTemplate.getForObject(URI.create(url), String.class);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("Empty response from Upbit API for market: {}", market);
                return List.of();
            }

            List<UpbitCandleDto> candles = objectMapper.readValue(
                response, 
                new TypeReference<List<UpbitCandleDto>>() {}
            );

            // 데이터 유효성 검증
            List<UpbitCandleDto> validCandles = candles.stream()
                .filter(UpbitCandleDto::isValid)
                .toList();

            if (validCandles.size() != candles.size()) {
                log.warn("Invalid candles filtered out. Original: {}, Valid: {}", 
                        candles.size(), validCandles.size());
            }

            log.info("Successfully fetched {} valid candles for market: {}", 
                    validCandles.size(), market);
            
            return validCandles;

        } catch (Exception e) {
            log.error("Failed to fetch candles from Upbit API for market: {}, error: {}", 
                     market, e.getMessage(), e);
            throw new RuntimeException("Upbit API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 최신 1개 캔들 데이터 조회
     * @param market 마켓 코드
     * @return 최신 캔들 데이터
     */
    public UpbitCandleDto getLatestCandle(String market) {
        List<UpbitCandleDto> candles = getCandles(market, 1);
        
        if (candles.isEmpty()) {
            throw new RuntimeException("최신 캔들 데이터를 조회할 수 없습니다: " + market);
        }
        
        return candles.get(0);
    }
}
