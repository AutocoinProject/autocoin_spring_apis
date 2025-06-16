package com.autocoin.upbit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "업비트 티커 정보")
public class UpbitTickerDto {
    
    @Schema(description = "마켓 코드")
    private String market;
    
    @Schema(description = "거래일")
    private String tradeDate;
    
    @Schema(description = "거래 시각")
    private String tradeTime;
    
    @Schema(description = "거래 일시(UTC)")
    private String tradeDateUtc;
    
    @Schema(description = "거래 시각(UTC)")
    private String tradeTimeUtc;
    
    @Schema(description = "타임스탬프")
    private Long timestamp;
    
    @Schema(description = "시가")
    private BigDecimal openingPrice;
    
    @Schema(description = "고가")
    private BigDecimal highPrice;
    
    @Schema(description = "저가")
    private BigDecimal lowPrice;
    
    @Schema(description = "현재가")
    private BigDecimal tradePrice;
    
    @Schema(description = "전일 종가")
    private BigDecimal prevClosingPrice;
    
    @Schema(description = "변화")
    private String change;
    
    @Schema(description = "변화량")
    private BigDecimal changePrice;
    
    @Schema(description = "변화율")
    private BigDecimal changeRate;
    
    @Schema(description = "체결 누적 거래대금")
    private BigDecimal accTradePrice;
    
    @Schema(description = "체결 누적 거래량")
    private BigDecimal accTradeVolume;
    
    @Schema(description = "체결 누적 거래대금(24시간)")
    private BigDecimal accTradePrice24h;
    
    @Schema(description = "체결 누적 거래량(24시간)")
    private BigDecimal accTradeVolume24h;
    
    @Schema(description = "52주 신고가")
    private BigDecimal highest52WeekPrice;
    
    @Schema(description = "52주 신고가 달성일")
    private String highest52WeekDate;
    
    @Schema(description = "52주 신저가")
    private BigDecimal lowest52WeekPrice;
    
    @Schema(description = "52주 신저가 달성일")
    private String lowest52WeekDate;
}