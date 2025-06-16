package com.autocoin.chart.api;

import com.autocoin.chart.application.ChartService;
import com.autocoin.chart.dto.LightweightChartDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 차트 데이터 REST API 컨트롤러
 * - 차트 데이터 조회 API 제공
 * - lightweight-charts 형태로 응답
 */
@Slf4j
@RestController
@RequestMapping("/api/chart")
@RequiredArgsConstructor
@Tag(name = "Chart API", description = "차트 데이터 조회 API")
public class ChartController {

    private final ChartService chartService;

    /**
     * 빠른 차트 데이터 조회 (거래소 방식)
     * - 캐시에서 즉시 반환, 외부 API 호출 없음
     * @param market 마켓 코드 (선택사항, 기본값: KRW-BTC)
     * @return lightweight-charts 형태의 캔들 데이터
     */
    @GetMapping("/fast")
    @Operation(
        summary = "빠른 차트 데이터 조회", 
        description = "캐시된 데이터를 즉시 반환합니다. 외부 API 호출 없이 0.1초 내 응답. 거래소 방식."
    )
    public ResponseEntity<List<LightweightChartDto>> getFastChartData(
            @Parameter(description = "마켓 코드 (예: KRW-BTC)", example = "KRW-BTC")
            @RequestParam(value = "market", required = false, defaultValue = "KRW-BTC") String market) {
        
        log.info("Fast chart data requested for market: {}", market);
        
        try {
            // 마켓 코드 검증
            if (market == null || market.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<LightweightChartDto> chartData = chartService.getFastChartData(market.trim().toUpperCase());
            
            if (chartData.isEmpty()) {
                log.warn("No fast chart data available for market: {}", market);
                return ResponseEntity.noContent().build();
            }

            log.info("Fast returning {} chart data points for market: {}", chartData.size(), market);
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            log.error("Error retrieving fast chart data for market: {}, error: {}", market, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🎆 DB에서 차트 데이터 조회 (영구 저장된 데이터)
     */
    @GetMapping("/database")
    @Operation(
        summary = "DB 차트 데이터 조회", 
        description = "데이터베이스에 영구 저장된 실제 업비트 차트 데이터를 조회합니다. Redis 우회 및 안정성 보장."
    )
    public ResponseEntity<List<LightweightChartDto>> getDatabaseChartData(
            @Parameter(description = "마켓 코드 (예: KRW-BTC)", example = "KRW-BTC")
            @RequestParam(value = "market", required = false, defaultValue = "KRW-BTC") String market,
            @Parameter(description = "캔듡 개수", example = "100")
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 마켓 코드 검증
            if (market == null || market.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // limit 제한
            limit = Math.min(Math.max(limit, 10), 1000);
            
            List<LightweightChartDto> chartData = chartService.getChartDataFromDatabase(market.trim().toUpperCase(), limit);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (chartData.isEmpty()) {
                log.warn("No database chart data available for market: {} after {}ms", market, duration);
                return ResponseEntity.noContent().build();
            }
            
            log.info("Database chart data loaded in {}ms with {} candles for market: {}", duration, chartData.size(), market);
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            log.error("Database chart data loading failed after {}ms for market: {}, error: {}", errorTime, market, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 더미 데이터 조회 (테스트용)
     */
    @GetMapping("/dummy")
    @Operation(
        summary = "더미 차트 데이터 조회", 
        description = "테스트용 더미 캔들 데이터를 생성하여 반환합니다. 실제 업비트 API 대신 사용할 수 있습니다."
    )
    public ResponseEntity<List<LightweightChartDto>> getDummyChartData(
            @Parameter(description = "생성할 캔들 개수", example = "100")
            @RequestParam(value = "count", required = false, defaultValue = "100") int count) {
        
        log.info("Dummy chart data requested with count: {}", count);
        
        try {
            // count 범위 검증
            if (count <= 0 || count > 1000) {
                log.warn("Invalid count value: {}, using default 100", count);
                count = 100;
            }

            List<LightweightChartDto> dummyData = chartService.generateDummyChartData(count);
            
            log.info("Returning {} dummy chart data points", dummyData.size());
            return ResponseEntity.ok(dummyData);

        } catch (Exception e) {
            log.error("Error generating dummy chart data, error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "차트 API 서버 상태 확인")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chart API is running");
    }

    /**
     * 초고속 차트 데이터 (캐시 우회, 즉시 생성)
     */
    @GetMapping("/instant")
    @Operation(
        summary = "초고속 차트 데이터", 
        description = "캐시와 외부 API를 우회하고 즉시 차트 데이터를 생성하여 반환합니다. 0.05초 내 응답 보장."
    )
    public ResponseEntity<List<LightweightChartDto>> getInstantChartData(
            @Parameter(description = "캔들 개수", example = "50")
            @RequestParam(value = "count", required = false, defaultValue = "50") int count) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // count 제한 (성능을 위해)
            count = Math.min(Math.max(count, 10), 100);
            
            List<LightweightChartDto> chartData = chartService.generateDummyChartData(count);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Instant chart data generated in {}ms with {} candles", duration, chartData.size());
            
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            log.error("Instant chart data generation failed after {}ms: {}", errorTime, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
