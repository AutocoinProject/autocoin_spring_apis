package com.autocoin.chart.application;

import com.autocoin.chart.dto.LightweightChartDto;
import com.autocoin.chart.dto.RealtimeChartDto;
import com.autocoin.chart.dto.UpbitCandleDto;
import com.autocoin.chart.infrastructure.CandleDataRepository;
import com.autocoin.chart.infrastructure.ChartUpbitApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 차트 데이터 서비스
 * - Upbit API에서 캔들 데이터 조회
 * - Redis에 데이터 저장
 * - WebSocket을 통한 실시간 데이터 전송
 * - 스케줄링을 통한 주기적 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartUpbitApiClient upbitApiClient;
    private final CandleDataRepository candleDataRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChartDatabaseService chartDatabaseService; // DB 서비스 추가

    @Value("${app.init.enabled:false}")
    private boolean initEnabled;

    private static final String DEFAULT_MARKET = "KRW-BTC";
    private static final String WEBSOCKET_TOPIC = "/topic/chart";

    /**
     * 애플리케이션 시작 시 초기 데이터 로드 (조건부)
     */
    @PostConstruct
    public void initializeData() {
        if (!initEnabled) {
            log.info("Chart data initialization is disabled (app.init.enabled=false)");
            return;
        }

        log.info("Initializing chart data on application startup");
        
        try {
            // 대량 히스토리 데이터 로드 (1000개)
            fetchAndStoreLargeHistoryData();
            log.info("Large history chart data loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load initial chart data: {}", e.getMessage(), e);
            // 실패 시 더미 데이터로 대체
            log.info("Loading dummy data as fallback");
            loadDummyDataAsFallback();
        }
    }

    /**
     * 1분마다 최신 캔들 데이터 업데이트
     * - 정확한 1분 주기로 실행
     * - Upbit API에서 최신 1개 캔들 조회
     * - Redis에 저장 및 WebSocket 전송
     */
    @Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
    public void updateCandleData() {
        if (!initEnabled) {
            log.debug("Chart data update is disabled (app.init.enabled=false)");
            return;
        }

        log.debug("Starting scheduled candle data update");
        
        try {
            // 최신 1개 캔들 데이터 조회
            UpbitCandleDto latestCandle = upbitApiClient.getLatestCandle(DEFAULT_MARKET);
            
            if (latestCandle == null || !latestCandle.isValid()) {
                log.warn("Invalid latest candle data received for market: {}", DEFAULT_MARKET);
                return;
            }

            // Redis에 저장
            candleDataRepository.addCandle(DEFAULT_MARKET, latestCandle);
            
            // 🎆 DB에도 저장
            chartDatabaseService.saveSingleCandle(latestCandle);

            // WebSocket으로 실시간 전송
            sendRealtimeUpdate(latestCandle);

            log.info("Successfully updated candle data for market: {} at {}", 
                    DEFAULT_MARKET, latestCandle.getCandleDateTimeUtc());

        } catch (Exception e) {
            log.error("Failed to update candle data: {}", e.getMessage(), e);
        }
    }

    /**
     * 빠른 차트 데이터 조회 (거래소 방식)
     * - 캐시에서 즉시 반환, 외부 API 호출 없음
     * @param market 마켓 코드
     * @return lightweight-charts 형태의 캔들 데이터
     */
    public List<LightweightChartDto> getFastChartData(String market) {
        String targetMarket = (market != null && !market.trim().isEmpty()) ? market : DEFAULT_MARKET;
        
        log.debug("Fast retrieving chart data for market: {}", targetMarket);
        
        try {
            // 캐시에서 즉시 조회 (외부 API 호출 없음)
            List<UpbitCandleDto> candles = candleDataRepository.getCandles(targetMarket);
            
            if (candles.isEmpty()) {
                log.warn("No cached data found for market: {}, returning empty list", targetMarket);
                return new ArrayList<>();
            }

            // lightweight-charts 형태로 변환
            List<LightweightChartDto> chartData = candles.stream()
                .map(LightweightChartDto::from)
                .sorted((a, b) -> Long.compare(a.getTime(), b.getTime())) // 시간 순 정렬
                .toList();

            log.info("Fast retrieved {} chart data points for market: {}", chartData.size(), targetMarket);
            return chartData;

        } catch (Exception e) {
            log.error("Failed to get fast chart data for market: {}, error: {}", targetMarket, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 대량 히스토리 데이터 로드 (1000개)
     * - 거래소 방식: 미리 대량 데이터 준비
     */
    private void fetchAndStoreLargeHistoryData() {
        log.info("Fetching large history data (1000 candles) for market: {}", DEFAULT_MARKET);
        
        try {
            // 1000개 캔들 데이터 가져오기
            List<UpbitCandleDto> historyCandles = upbitApiClient.getCandles(DEFAULT_MARKET, 1000);
            
            if (historyCandles.isEmpty()) {
                log.warn("No history data received, using dummy data instead");
                loadDummyDataAsFallback();
                return;
            }

            // Redis에 대량 저장
            candleDataRepository.saveCandles(DEFAULT_MARKET, historyCandles);
            
            // 🎆 DB에도 저장 (영구 보관)
            chartDatabaseService.saveUpbitCandles(historyCandles);
            
            log.info("Saved {} history candles for fast loading (Redis + DB)", historyCandles.size());
            
        } catch (Exception e) {
            log.error("Failed to fetch large history data: {}", e.getMessage(), e);
            // 실패 시 더미 데이터로 대체
            loadDummyDataAsFallback();
        }
    }
    
    /**
     * 더미 데이터로 대체 (외부 API 실패 시)
     */
    private void loadDummyDataAsFallback() {
        log.info("Loading 500 dummy candles as fallback data");
        
        try {
            List<LightweightChartDto> dummyData = generateDummyChartData(500);
            
            // LightweightChartDto를 UpbitCandleDto로 변환해서 저장
            List<UpbitCandleDto> dummyCandles = dummyData.stream()
                .map(this::convertToUpbitCandle)
                .toList();
                
            candleDataRepository.saveCandles(DEFAULT_MARKET, dummyCandles);
            log.info("Saved {} dummy candles for development", dummyCandles.size());
            
        } catch (Exception e) {
            log.error("Failed to load dummy data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * LightweightChartDto를 UpbitCandleDto로 변환
     */
    private UpbitCandleDto convertToUpbitCandle(LightweightChartDto dto) {
        UpbitCandleDto candle = new UpbitCandleDto();
        candle.setMarket(DEFAULT_MARKET);
        candle.setOpeningPrice(dto.getOpen());
        candle.setHighPrice(dto.getHigh());
        candle.setLowPrice(dto.getLow());
        candle.setTradePrice(dto.getClose());
        
        // Unix timestamp를 UTC 시간 문자열로 변환
        java.time.Instant instant = java.time.Instant.ofEpochSecond(dto.getTime());
        String utcDateTime = instant.atZone(java.time.ZoneOffset.UTC)
            .toLocalDateTime()
            .toString(); // 2023-01-01T00:00:00 형식
        candle.setCandleDateTimeUtc(utcDateTime);
        
        // 타임스탬프도 설정 (milliseconds)
        candle.setTimestamp(dto.getTime() * 1000);
        
        return candle;
    }

    /**
     * 더미 차트 데이터 생성 (테스트용)
     * @param count 생성할 캔들 개수
     * @return lightweight-charts 형태의 더미 데이터
     */
    public List<LightweightChartDto> generateDummyChartData(int count) {
        log.info("Generating {} dummy chart data points", count);
        
        List<LightweightChartDto> dummyData = new ArrayList<>();
        
        // 기본 설정
        BigDecimal basePrice = new BigDecimal("95000000"); // 9천5백만원 (비트코인 기준)
        long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp (초)
        Random random = new Random();
        
        for (int i = count - 1; i >= 0; i--) {
            long time = currentTime - (i * 60); // 1분 간격
            
            // 변동성 2%
            double volatility = 0.02;
            
            // 시가 생성
            double openVariation = (random.nextDouble() - 0.5) * volatility;
            BigDecimal open = basePrice.multiply(BigDecimal.valueOf(1 + openVariation));
            
            // 종가 생성 (시가 기준 적은 변동)
            double closeVariation = (random.nextDouble() - 0.5) * volatility * 0.5;
            BigDecimal close = open.multiply(BigDecimal.valueOf(1 + closeVariation));
            
            // 고가 생성
            BigDecimal maxOfOpenClose = open.max(close);
            double highVariation = random.nextDouble() * volatility * 0.3;
            BigDecimal high = maxOfOpenClose.multiply(BigDecimal.valueOf(1 + highVariation));
            
            // 저가 생성
            BigDecimal minOfOpenClose = open.min(close);
            double lowVariation = random.nextDouble() * volatility * 0.3;
            BigDecimal low = minOfOpenClose.multiply(BigDecimal.valueOf(1 - lowVariation));
            
            // 정수로 반올림
            open = open.setScale(0, RoundingMode.HALF_UP);
            high = high.setScale(0, RoundingMode.HALF_UP);
            low = low.setScale(0, RoundingMode.HALF_UP);
            close = close.setScale(0, RoundingMode.HALF_UP);
            
            LightweightChartDto candle = new LightweightChartDto();
            candle.setTime(time);
            candle.setOpen(open);
            candle.setHigh(high);
            candle.setLow(low);
            candle.setClose(close);
            dummyData.add(candle);
        }
        
        // 시간 순으로 정렬
        dummyData.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
        
        log.info("Generated {} dummy candles, time range: {} to {}", 
                dummyData.size(),
                dummyData.isEmpty() ? "N/A" : java.time.Instant.ofEpochSecond(dummyData.get(0).getTime()),
                dummyData.isEmpty() ? "N/A" : java.time.Instant.ofEpochSecond(dummyData.get(dummyData.size() - 1).getTime()));
        
        return dummyData;
    }

    /**
     * 🎆 DB에서 차트 데이터 조회 (영구 저장된 데이터)
     */
    public List<LightweightChartDto> getChartDataFromDatabase(String market, int limit) {
        String targetMarket = (market != null && !market.trim().isEmpty()) ? market : DEFAULT_MARKET;
        
        log.debug("🎆 Retrieving chart data from database for market: {} (limit: {})", targetMarket, limit);
        
        try {
            return chartDatabaseService.getChartDataFromDatabase(targetMarket, limit);
            
        } catch (Exception e) {
            log.error("Failed to get chart data from database for market: {}, error: {}", targetMarket, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * WebSocket을 통한 실시간 업데이트 전송
     */
    private void sendRealtimeUpdate(UpbitCandleDto candle) {
        try {
            LightweightChartDto chartData = LightweightChartDto.from(candle);
            RealtimeChartDto realtimeData = RealtimeChartDto.createCandleUpdate(DEFAULT_MARKET, chartData);

            // 특정 마켓 채널로 전송
            String topic = WEBSOCKET_TOPIC + "/" + DEFAULT_MARKET;
            messagingTemplate.convertAndSend(topic, realtimeData);

            log.debug("Sent realtime update to WebSocket topic: {}", topic);

        } catch (Exception e) {
            log.error("Failed to send realtime update via WebSocket: {}", e.getMessage(), e);
        }
    }
}
