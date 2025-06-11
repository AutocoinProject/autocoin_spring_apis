package com.autocoin.chart.application;

import com.autocoin.chart.domain.entity.ChartCandle;
import com.autocoin.chart.domain.repository.ChartCandleJpaRepository;
import com.autocoin.chart.dto.LightweightChartDto;
import com.autocoin.chart.dto.UpbitCandleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * 차트 데이터 DB 저장 서비스
 * - Redis 캐시와 함께 DB에도 영구 저장
 * - 데이터 안정성 및 히스토리 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChartDatabaseService {

    private final ChartCandleJpaRepository chartCandleRepository;

    /**
     * 업비트 캔들 데이터를 DB에 저장 (배치 최적화)
     */
    public void saveUpbitCandles(List<UpbitCandleDto> upbitCandles) {
        if (upbitCandles == null || upbitCandles.isEmpty()) {
            log.warn("No candles to save to database");
            return;
        }

        try {
            log.info("💾 Saving {} candles to database (batch optimized)", upbitCandles.size());

            // 유효한 캔들만 필터링
            List<UpbitCandleDto> validCandles = upbitCandles.stream()
                .filter(candle -> {
                    if (!candle.isValid()) {
                        log.warn("Invalid candle data skipped: {}", candle.getMarket());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

            if (validCandles.isEmpty()) {
                log.warn("No valid candles to save after filtering");
                return;
            }

            // 배치로 기존 캔들 확인 - 효율적인 방식
            Set<String> existingKeys = getExistingCandleKeysBatch(validCandles);
            
            // 새로운 캔들만 필터링
            List<UpbitCandleDto> newCandles = validCandles.stream()
                .filter(candle -> !existingKeys.contains(
                    candle.getMarket() + "_" + candle.getUnixTimestamp()))
                .collect(Collectors.toList());
            
            if (newCandles.isEmpty()) {
                log.info("⏭️ All {} candles already exist in database", validCandles.size());
                return;
            }
            
            log.info("💾 Found {} new candles out of {} total", newCandles.size(), validCandles.size());
            
            // 새로운 캔들들을 ChartCandle로 변환하여 배치 저장
            List<ChartCandle> chartCandles = newCandles.stream()
                .map(upbitCandle -> {
                    try {
                        return ChartCandle.fromUpbitCandle(upbitCandle);
                    } catch (Exception e) {
                        log.warn("Failed to convert candle: {} - {}", upbitCandle.getMarket(), e.getMessage());
                        return null;
                    }
                })
                .filter(candle -> candle != null)
                .collect(Collectors.toList());
            
            if (chartCandles.isEmpty()) {
                log.warn("No chart candles to save after conversion");
                return;
            }

            chartCandleRepository.saveAll(chartCandles);
            log.info("✅ Database save completed for {} new candles", chartCandles.size());

        } catch (Exception e) {
            log.error("❌ Failed to save candles to database: {}", e.getMessage(), e);
            throw new RuntimeException("차트 데이터 DB 저장 실패", e);
        }
    }
    
    /**
     * 배치로 기존 캔들 키들을 조회하여 중복 체크 최적화
     * 개별 쿼리 대신 마켓별로 묶어서 처리
     */
    private Set<String> getExistingCandleKeysBatch(List<UpbitCandleDto> candles) {
        if (candles.isEmpty()) {
            return Collections.emptySet();
        }
        
        // 마켓별로 그룹화
        var marketGroups = candles.stream()
            .collect(Collectors.groupingBy(UpbitCandleDto::getMarket));
        
        Set<String> existingKeys = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        
        // 마켓별로 배치 처리
        marketGroups.forEach((market, marketCandles) -> {
            Set<Long> candleTimes = marketCandles.stream()
                .map(UpbitCandleDto::getUnixTimestamp)
                .collect(Collectors.toSet());
            
            // 마켓별로 기존 데이터 조회
            List<ChartCandle> existing = chartCandleRepository.findByMarketAndCandleTimeIn(market, candleTimes);
            
            // 기존 키 생성
            existing.forEach(candle -> 
                existingKeys.add(candle.getMarket() + "_" + candle.getCandleTime())
            );
        });
        
        return existingKeys;
    }

    /**
     * 단일 캔들 데이터 저장 (기존 방식 유지 - 단일 저장용)
     */
    public void saveSingleCandle(UpbitCandleDto upbitCandle) {
        if (upbitCandle == null || !upbitCandle.isValid()) {
            log.warn("Invalid candle data provided for database save");
            return;
        }

        try {
            // 중복 체크
            boolean exists = chartCandleRepository.existsByMarketAndCandleTime(
                upbitCandle.getMarket(), 
                upbitCandle.getUnixTimestamp()
            );

            if (!exists) {
                ChartCandle chartCandle = ChartCandle.fromUpbitCandle(upbitCandle);
                chartCandleRepository.save(chartCandle);
                log.info("💾 New candle saved to DB: {} at {}", upbitCandle.getMarket(), upbitCandle.getUnixTimestamp());
            } else {
                log.debug("⏭️ Candle already exists in DB: {} at {}", upbitCandle.getMarket(), upbitCandle.getUnixTimestamp());
            }

        } catch (Exception e) {
            log.error("❌ Failed to save single candle to database: {}", e.getMessage(), e);
        }
    }

    /**
     * DB에서 차트 데이터 조회
     */
    @Transactional(readOnly = true)
    public List<LightweightChartDto> getChartDataFromDatabase(String market, int limit) {
        try {
            log.info("📖 Loading chart data from database: {} (limit: {})", market, limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<ChartCandle> candles = chartCandleRepository.findByMarketOrderByCandleTimeDesc(market, pageable);

            List<LightweightChartDto> chartData = candles.stream()
                .map(ChartCandle::toLightweightChartDto)
                .toList();

            log.info("✅ Loaded {} chart data points from database for market: {}", chartData.size(), market);
            return chartData;

        } catch (Exception e) {
            log.error("❌ Failed to load chart data from database: {}", e.getMessage(), e);
            throw new RuntimeException("차트 데이터 DB 조회 실패", e);
        }
    }

    /**
     * DB 저장된 데이터 현황 조회
     */
    @Transactional(readOnly = true)
    public DatabaseSummary getDatabaseSummary(String market) {
        try {
            long totalCount = chartCandleRepository.countByMarket(market);
            
            ChartCandle latestCandle = chartCandleRepository.findFirstByMarketOrderByCandleTimeDesc(market).orElse(null);
            
            return DatabaseSummary.builder()
                .market(market)
                .totalCandles(totalCount)
                .latestCandleTime(latestCandle != null ? latestCandle.getCandleTime() : null)
                .latestPrice(latestCandle != null ? latestCandle.getClosePrice() : null)
                .hasData(totalCount > 0)
                .build();

        } catch (Exception e) {
            log.error("❌ Failed to get database summary: {}", e.getMessage(), e);
            return DatabaseSummary.empty(market);
        }
    }

    /**
     * 데이터베이스 요약 정보 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class DatabaseSummary {
        private String market;
        private long totalCandles;
        private Long latestCandleTime;
        private java.math.BigDecimal latestPrice;
        private boolean hasData;

        public static DatabaseSummary empty(String market) {
            return DatabaseSummary.builder()
                .market(market)
                .totalCandles(0)
                .hasData(false)
                .build();
        }
    }

    /**
     * 오래된 데이터 정리 (옵션)
     */
    public void cleanupOldData(String market, int keepCount) {
        try {
            long totalCount = chartCandleRepository.countByMarket(market);
            
            if (totalCount > keepCount) {
                log.info("🧹 Cleaning up old data for market: {} (keeping latest {} out of {})", 
                        market, keepCount, totalCount);
                
                // 여기서는 단순히 로그만 남기고, 실제 삭제는 별도 배치 작업으로 처리
                log.info("📊 Cleanup candidate: {} old candles", totalCount - keepCount);
            }

        } catch (Exception e) {
            log.error("❌ Failed to cleanup old data: {}", e.getMessage(), e);
        }
    }
}
