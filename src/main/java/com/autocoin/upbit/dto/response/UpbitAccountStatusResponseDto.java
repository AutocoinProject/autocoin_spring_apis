package com.autocoin.upbit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "업비트 계정 상태 응답")
public class UpbitAccountStatusResponseDto {
    
    @Schema(description = "연결 여부")
    private boolean connected;
    
    @Schema(description = "계정 상태")
    private String accountState;
    
    @Schema(description = "마지막 동기화 시간")
    private LocalDateTime lastSyncAt;
    
    @Schema(description = "계정 별명")
    private String nickname;
}