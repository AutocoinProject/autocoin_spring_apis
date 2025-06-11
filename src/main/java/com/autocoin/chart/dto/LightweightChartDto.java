package com.autocoin.chart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * lightweight-charts 형태의 캔들 데이터 DTO
 * - 프론트엔드 차트 라이브러리용 형태
 * - Unix timestamp 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LightweightChartDto {

    private long time; // Unix timestamp (seconds)
    private BigDecimal open; // 시가
    private BigDecimal high; // 고가
    private BigDecimal low; // 저가
    private BigDecimal close; // 종가

    /**
     * Upbit 캔들 데이터를 lightweight-charts 형태로 변환
     * @param upbitCandle Upbit 캔들 데이터
     * @return lightweight-charts DTO
     */
    public static LightweightChartDto from(UpbitCandleDto upbitCandle) {
        LightweightChartDto dto = new LightweightChartDto();
        dto.setTime(upbitCandle.getUnixTimestamp());
        dto.setOpen(upbitCandle.getOpeningPrice());
        dto.setHigh(upbitCandle.getHighPrice());
        dto.setLow(upbitCandle.getLowPrice());
        dto.setClose(upbitCandle.getTradePrice());
        return dto;
    }
}
