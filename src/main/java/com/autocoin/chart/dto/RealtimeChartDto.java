package com.autocoin.chart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * WebSocket을 통해 전송되는 실시간 차트 데이터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeChartDto {

    private String market; // KRW-BTC
    private long timestamp; // Unix timestamp (seconds)
    private BigDecimal price; // 현재 가격
    private String type; // 데이터 타입 ("candle", "price")
    private LightweightChartDto candleData; // 캔들 데이터 (type이 "candle"일 때)

    /**
     * 실시간 가격 데이터 생성
     */
    public static RealtimeChartDto createPriceUpdate(String market, BigDecimal price) {
        return new RealtimeChartDto(
            market,
            System.currentTimeMillis() / 1000,
            price,
            "price",
            null
        );
    }

    /**
     * 실시간 캔들 데이터 생성
     */
    public static RealtimeChartDto createCandleUpdate(String market, LightweightChartDto candleData) {
        return new RealtimeChartDto(
            market,
            candleData.getTime(),
            candleData.getClose(),
            "candle",
            candleData
        );
    }
}
