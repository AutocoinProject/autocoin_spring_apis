package com.autocoin.chart.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Upbit API 캔들 데이터 응답 DTO
 * - Upbit API 원본 형태 그대로 매핑
 * - UTC 시간 저장
 * - 알 수 없는 필드 무시 처리
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpbitCandleDto {

    @JsonProperty("market")
    private String market; // KRW-BTC

    @JsonProperty("candle_date_time_utc")
    private String candleDateTimeUtc; // 2023-01-01T00:00:00

    @JsonProperty("candle_date_time_kst")
    private String candleDateTimeKst; // 2023-01-01T09:00:00

    @JsonProperty("opening_price")
    private BigDecimal openingPrice; // 시가

    @JsonProperty("high_price")
    private BigDecimal highPrice; // 고가

    @JsonProperty("low_price")
    private BigDecimal lowPrice; // 저가

    @JsonProperty("trade_price")
    private BigDecimal tradePrice; // 종가

    @JsonProperty("timestamp")
    private Long timestamp; // Unix timestamp (milliseconds)

    @JsonProperty("candle_acc_trade_price")
    private BigDecimal candleAccTradePrice; // 누적 거래금액

    @JsonProperty("candle_acc_trade_volume")
    private BigDecimal candleAccTradeVolume; // 누적 거래량

    @JsonProperty("prev_closing_price")
    private BigDecimal prevClosingPrice; // 전일 종가

    @JsonProperty("change_price")
    private BigDecimal changePrice; // 전일 대비 가격 변동

    @JsonProperty("change_rate")
    private BigDecimal changeRate; // 전일 대비 변동률

    /**
     * 데이터 유효성 검증
     * @return 유효한 데이터인지 여부
     */
    public boolean isValid() {
        return market != null && 
               !market.trim().isEmpty() &&
               openingPrice != null && 
               openingPrice.compareTo(BigDecimal.ZERO) > 0 &&
               highPrice != null && 
               highPrice.compareTo(BigDecimal.ZERO) > 0 &&
               lowPrice != null && 
               lowPrice.compareTo(BigDecimal.ZERO) > 0 &&
               tradePrice != null && 
               tradePrice.compareTo(BigDecimal.ZERO) > 0 &&
               candleDateTimeUtc != null &&
               !candleDateTimeUtc.trim().isEmpty();
    }

    /**
     * UTC 시간을 Unix timestamp로 변환
     * @return Unix timestamp (seconds)
     */
    public long getUnixTimestamp() {
        if (timestamp != null) {
            return timestamp / 1000; // milliseconds -> seconds
        }
        
        // timestamp가 없으면 candleDateTimeUtc를 파싱
        try {
            LocalDateTime dateTime = LocalDateTime.parse(candleDateTimeUtc);
            return dateTime.atZone(java.time.ZoneOffset.UTC).toEpochSecond();
        } catch (Exception e) {
            return System.currentTimeMillis() / 1000;
        }
    }
}
