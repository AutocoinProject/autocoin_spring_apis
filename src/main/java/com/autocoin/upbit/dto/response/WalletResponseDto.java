package com.autocoin.upbit.dto.response;

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
@Schema(description = "지갑 정보 응답")
public class WalletResponseDto {
    
    @Schema(description = "화폐 코드")
    private String currency;
    
    @Schema(description = "잔고")
    private BigDecimal balance;
    
    @Schema(description = "주문 중 잔고")
    private BigDecimal locked;
    
    @Schema(description = "평균 매수가")
    private BigDecimal avgBuyPrice;
    
    @Schema(description = "평균 매수가 수정 여부")
    private boolean avgBuyPriceModified;
    
    @Schema(description = "화폐 단위")
    private String unitCurrency;
}