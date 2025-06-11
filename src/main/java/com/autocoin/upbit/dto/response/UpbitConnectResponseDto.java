package com.autocoin.upbit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "업비트 계정 연결 응답")
public class UpbitConnectResponseDto {
    
    @Schema(description = "연결 성공 여부")
    private boolean success;
    
    @Schema(description = "응답 메시지")
    private String message;
    
    @Schema(description = "계정 상태")
    private String accountState;
    
    @Schema(description = "계정 별명")
    private String nickname;
}