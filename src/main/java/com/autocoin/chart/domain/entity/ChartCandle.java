package com.autocoin.chart.domain.entity;

import com.autocoin.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 차트 캔들 데이터 엔티티
 * - 업비트에서 가져온 캔들 데이터를 DB에 저장
 * - Redis 캐시와 함께 사용하여 안정성 확보
 */
@Entity
@Table(
    name = "chart_candles",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_chart_candle_market_time",
            columnNames = {"market", "candleTime"}
        )
    },
    indexes = {
        @Index(name = "idx_chart_candle_market", columnList = "market"),
        @Index(name = "idx_chart_candle_time", columnList = "candleTime"),
        @Index(name = "idx_chart_candle_market_time", columnList = "market, candleTime")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChartCandle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String market; // KRW-BTC

    @Column(nullable = false)
    private Long candleTime; // Unix timestamp (seconds)

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal openPrice; // 시가

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal highPrice; // 고가

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal lowPrice; // 저가

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal closePrice; // 종가

    @Column(precision = 20, scale = 8)
    private BigDecimal volume; // 거래량

    @Column(nullable = true, length = 30) // nullable로 변경
    private String candleDateTimeUtc; // UTC 시간 문자열

    @Column(nullable = false)
    private Long timestamp; // 원본 타임스탬프 (milliseconds)

    @Builder
    public ChartCandle(
            String market,
            Long candleTime,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            BigDecimal volume,
            String candleDateTimeUtc,
            Long timestamp
    ) {
        this.market = market;
        this.candleTime = candleTime;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.candleDateTimeUtc = candleDateTimeUtc;
        this.timestamp = timestamp;
    }

    /**
     * UpbitCandleDto에서 ChartCandle 엔티티로 변환
     */
    public static ChartCandle fromUpbitCandle(com.autocoin.chart.dto.UpbitCandleDto upbitCandle) {
        // candleDateTimeUtc가 null이거나 비어있으면 timestamp로부터 생성
        String utcDateTime = upbitCandle.getCandleDateTimeUtc();
        if (utcDateTime == null || utcDateTime.trim().isEmpty()) {
            if (upbitCandle.getTimestamp() != null) {
                utcDateTime = Instant.ofEpochMilli(upbitCandle.getTimestamp())
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }

        return ChartCandle.builder()
                .market(upbitCandle.getMarket())
                .candleTime(upbitCandle.getUnixTimestamp())
                .openPrice(upbitCandle.getOpeningPrice())
                .highPrice(upbitCandle.getHighPrice())
                .lowPrice(upbitCandle.getLowPrice())
                .closePrice(upbitCandle.getTradePrice())
                .volume(upbitCandle.getCandleAccTradeVolume())
                .candleDateTimeUtc(utcDateTime)
                .timestamp(upbitCandle.getTimestamp() != null ? upbitCandle.getTimestamp() : upbitCandle.getUnixTimestamp() * 1000)
                .build();
    }

    /**
     * LightweightChartDto로 변환
     */
    public com.autocoin.chart.dto.LightweightChartDto toLightweightChartDto() {
        com.autocoin.chart.dto.LightweightChartDto dto = new com.autocoin.chart.dto.LightweightChartDto();
        dto.setTime(this.candleTime);
        dto.setOpen(this.openPrice);
        dto.setHigh(this.highPrice);
        dto.setLow(this.lowPrice);
        dto.setClose(this.closePrice);
        return dto;
    }
}
