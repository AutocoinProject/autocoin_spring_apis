package com.autocoin.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingStartRequestDto {
    
    @NotBlank(message = "전략은 필수입니다")
    private String strategy;
    
    @NotBlank(message = "심볼은 필수입니다")
    private String symbol;
    
    @NotNull(message = "금액은 필수입니다")
    @Positive(message = "금액은 0보다 커야 합니다")
    private Double amount;
    
    @Builder.Default
    private Double stopLoss = 0.05;  // 기본 5%
    
    @Builder.Default
    private Double takeProfit = 0.1; // 기본 10%
}
