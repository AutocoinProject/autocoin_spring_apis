package com.autocoin.chart.infrastructure;

import com.autocoin.chart.dto.UpbitCandleDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 캔들 데이터 Redis 저장소
 * - Redis 실패 시 in-memory 저장소로 fallback
 * - FIFO 방식으로 최신 100개 유지
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CandleDataRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis 실패 시 fallback용 in-memory 저장소
    private final ConcurrentHashMap<String, List<UpbitCandleDto>> inMemoryStorage = new ConcurrentHashMap<>();

    private static final int MAX_CANDLES = 100;
    private static final Duration TTL = Duration.ofMinutes(30);

    /**
     * 캔들 데이터 저장/업데이트
     * @param market 마켓 코드
     * @param newCandles 새로운 캔들 데이터 리스트
     */
    public void saveCandles(String market, List<UpbitCandleDto> newCandles) {
        if (newCandles == null || newCandles.isEmpty()) {
            log.warn("Empty candles provided for market: {}", market);
            return;
        }

        String key = generateKey(market);

        try {
            // Redis에서 기존 데이터 조회
            List<UpbitCandleDto> existingCandles = getCandlesFromRedis(key);
            
            // 새 데이터와 기존 데이터 병합 및 정렬 (최신순)
            List<UpbitCandleDto> mergedCandles = mergeAndLimitCandles(existingCandles, newCandles);
            
            // Redis에 저장
            redisTemplate.opsForValue().set(key, mergedCandles, TTL);
            
            log.debug("Saved {} candles to Redis for market: {}", mergedCandles.size(), market);

        } catch (Exception e) {
            log.error("Failed to save candles to Redis for market: {}, falling back to in-memory storage. Error: {}", 
                     market, e.getMessage());
            
            // Fallback: in-memory 저장
            saveToInMemory(market, newCandles);
        }
    }

    /**
     * 단일 캔들 데이터 추가
     * @param market 마켓 코드
     * @param newCandle 새로운 캔들 데이터
     */
    public void addCandle(String market, UpbitCandleDto newCandle) {
        if (newCandle == null || !newCandle.isValid()) {
            log.warn("Invalid candle provided for market: {}", market);
            return;
        }

        saveCandles(market, List.of(newCandle));
    }

    /**
     * 캔들 데이터 조회
     * @param market 마켓 코드
     * @return 캔들 데이터 리스트 (최신순)
     */
    public List<UpbitCandleDto> getCandles(String market) {
        String key = generateKey(market);

        try {
            // Redis에서 조회
            List<UpbitCandleDto> candles = getCandlesFromRedis(key);
            
            if (!candles.isEmpty()) {
                log.debug("Retrieved {} candles from Redis for market: {}", candles.size(), market);
                return candles;
            }

        } catch (Exception e) {
            log.error("Failed to get candles from Redis for market: {}, trying in-memory storage. Error: {}", 
                     market, e.getMessage());
        }

        // Fallback: in-memory에서 조회
        List<UpbitCandleDto> inMemoryCandles = inMemoryStorage.getOrDefault(market, List.of());
        log.debug("Retrieved {} candles from in-memory storage for market: {}", inMemoryCandles.size(), market);
        
        return new ArrayList<>(inMemoryCandles);
    }

    /**
     * Redis 키 생성
     */
    private String generateKey(String market) {
        return "candles:" + market;
    }

    /**
     * Redis에서 캔들 데이터 조회
     */
    @SuppressWarnings("unchecked")
    private List<UpbitCandleDto> getCandlesFromRedis(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                return new ArrayList<>();
            }

            if (value instanceof List) {
                // 직접 List로 저장된 경우
                return ((List<?>) value).stream()
                    .filter(item -> item instanceof UpbitCandleDto)
                    .map(item -> (UpbitCandleDto) item)
                    .toList();
            } else {
                // JSON 문자열로 저장된 경우
                String jsonValue = value.toString();
                return objectMapper.readValue(jsonValue, new TypeReference<List<UpbitCandleDto>>() {});
            }

        } catch (Exception e) {
            log.error("Failed to deserialize candles from Redis key: {}, error: {}", key, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 새 데이터와 기존 데이터를 병합하고 최신 100개로 제한
     */
    private List<UpbitCandleDto> mergeAndLimitCandles(List<UpbitCandleDto> existing, List<UpbitCandleDto> newCandles) {
        List<UpbitCandleDto> merged = new ArrayList<>(existing);
        
        // 새 데이터 추가 (중복 제거)
        for (UpbitCandleDto newCandle : newCandles) {
            boolean isDuplicate = merged.stream()
                .anyMatch(existing_candle -> 
                    existing_candle.getCandleDateTimeUtc().equals(newCandle.getCandleDateTimeUtc()));
            
            if (!isDuplicate) {
                merged.add(newCandle);
            }
        }

        // 시간 순으로 정렬 (최신순)
        merged.sort((c1, c2) -> c2.getCandleDateTimeUtc().compareTo(c1.getCandleDateTimeUtc()));

        // 최신 100개로 제한 (새로운 ArrayList 생성하여 SubList 문제 해결)
        if (merged.size() > MAX_CANDLES) {
            merged = new ArrayList<>(merged.subList(0, MAX_CANDLES));
        }

        return merged;
    }

    /**
     * In-memory 저장소에 저장 (fallback)
     */
    private void saveToInMemory(String market, List<UpbitCandleDto> newCandles) {
        List<UpbitCandleDto> existing = inMemoryStorage.getOrDefault(market, new ArrayList<>());
        List<UpbitCandleDto> merged = mergeAndLimitCandles(existing, newCandles);
        
        inMemoryStorage.put(market, merged);
        log.debug("Saved {} candles to in-memory storage for market: {}", merged.size(), market);
    }

    /**
     * 손상된 Redis 캐시 데이터 정리 (개발용)
     * SubList 직렬화 문제로 인한 손상된 데이터 제거
     */
    public void clearCorruptedRedisData(String market) {
        String key = generateKey(market);
        try {
            redisTemplate.delete(key);
            log.info("Cleared corrupted Redis data for market: {}", market);
        } catch (Exception e) {
            log.error("Failed to clear Redis data for market: {}, error: {}", market, e.getMessage());
        }
    }

    /**
     * 모든 캐시 데이터 정리 (개발용)
     */
    public void clearAllCacheData() {
        try {
            // Redis 데이터 정리
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            
            // In-memory 데이터 정리
            inMemoryStorage.clear();
            
            log.info("Cleared all cache data (Redis + In-memory)");
        } catch (Exception e) {
            log.error("Failed to clear all cache data, error: {}", e.getMessage());
        }
    }
}
