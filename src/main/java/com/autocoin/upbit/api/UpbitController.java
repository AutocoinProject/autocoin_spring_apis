package com.autocoin.upbit.api;

import com.autocoin.upbit.application.UpbitService;
import com.autocoin.upbit.dto.UpbitTickerDto;
import com.autocoin.upbit.dto.request.UpbitConnectRequestDto;
import com.autocoin.upbit.dto.response.UpbitAccountStatusResponseDto;
import com.autocoin.upbit.dto.response.UpbitConnectResponseDto;
import com.autocoin.upbit.dto.response.WalletResponseDto;
import com.autocoin.upbit.infrastructure.UpbitApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upbit")
@RequiredArgsConstructor
@Tag(name = "Upbit", description = "업비트 연동 API")
public class UpbitController {
    
    private final UpbitService upbitService;
    private final UpbitApiClient upbitApiClient;
    
    @PostMapping("/connect")
    @Operation(summary = "업비트 계정 연결", description = "업비트 API 키를 사용하여 계정을 연결합니다.")
    public ResponseEntity<UpbitConnectResponseDto> connectAccount(
            @Valid @RequestBody UpbitConnectRequestDto request,
            Authentication authentication) {
        UpbitConnectResponseDto response = upbitService.connectUpbitAccount(request, authentication);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    @Operation(summary = "업비트 계정 상태 조회", description = "연결된 업비트 계정의 상태를 조회합니다.")
    public ResponseEntity<UpbitAccountStatusResponseDto> getAccountStatus(Authentication authentication) {
        UpbitAccountStatusResponseDto response = upbitService.getUpbitAccountStatus(authentication);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/wallet")
    @Operation(summary = "지갑 정보 조회", description = "업비트 지갑의 잔고 정보를 조회합니다.")
    public ResponseEntity<List<WalletResponseDto>> getWalletInfo(Authentication authentication) {
        List<WalletResponseDto> walletInfo = upbitService.getWalletInfo(authentication);
        return ResponseEntity.ok(walletInfo);
    }
    
    @DeleteMapping("/disconnect")
    @Operation(summary = "업비트 계정 연결 해제", description = "연결된 업비트 계정의 연결을 해제합니다.")
    public ResponseEntity<Map<String, String>> disconnectAccount(Authentication authentication) {
        upbitService.disconnectUpbitAccount(authentication);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "업비트 계정 연결이 해제되었습니다."
        ));
    }
    
    @PostMapping("/sync")
    @Operation(summary = "계정 상태 동기화", description = "업비트 계정 상태를 동기화합니다.")
    public ResponseEntity<Map<String, String>> syncAccount(Authentication authentication) {
        upbitService.syncAccountStatus(authentication);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "계정 상태가 동기화되었습니다."
        ));
    }
    
    // 공개 API 엔드포인트들 (인증 불필요)
    
    @GetMapping("/markets")
    @Operation(summary = "마켓 코드 조회", description = "업비트에서 거래 가능한 마켓 목록을 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getMarkets() {
        List<Map<String, Object>> markets = upbitApiClient.getMarkets();
        return ResponseEntity.ok(markets);
    }
    
    @GetMapping("/ticker")
    @Operation(summary = "시세 정보 조회", description = "특정 마켓들의 현재 시세 정보를 조회합니다.")
    public ResponseEntity<List<UpbitTickerDto>> getTickers(
            @Parameter(description = "마켓 코드 목록 (쉼표로 구분)", example = "KRW-BTC,KRW-ETH")
            @RequestParam String markets) {
        List<String> marketList = Arrays.asList(markets.split(","));
        List<UpbitTickerDto> tickers = upbitService.getMarketTickers(marketList);
        return ResponseEntity.ok(tickers);
    }
    
    @GetMapping("/orderbook")
    @Operation(summary = "호가 정보 조회", description = "특정 마켓들의 호가 정보를 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getOrderbook(
            @Parameter(description = "마켓 코드 목록 (쉼표로 구분)", example = "KRW-BTC,KRW-ETH")
            @RequestParam String markets) {
        List<String> marketList = Arrays.asList(markets.split(","));
        List<Map<String, Object>> orderbook = upbitApiClient.getOrderbook(marketList);
        return ResponseEntity.ok(orderbook);
    }
    
    @GetMapping("/candles/days")
    @Operation(summary = "일봉 조회", description = "특정 마켓의 일봉 데이터를 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getDayCandles(
            @Parameter(description = "마켓 코드", example = "KRW-BTC")
            @RequestParam String market,
            @Parameter(description = "마지막 캔들 시각")
            @RequestParam(required = false) String to,
            @Parameter(description = "캔들 개수")
            @RequestParam(defaultValue = "200") int count) {
        List<Map<String, Object>> candles = upbitApiClient.getDayCandles(market, to, count);
        return ResponseEntity.ok(candles);
    }
    
    @GetMapping("/candles/minutes/{unit}")
    @Operation(summary = "분봉 조회", description = "특정 마켓의 분봉 데이터를 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getMinuteCandles(
            @Parameter(description = "분 단위", example = "1")
            @PathVariable int unit,
            @Parameter(description = "마켓 코드", example = "KRW-BTC")
            @RequestParam String market,
            @Parameter(description = "마지막 캔들 시각")
            @RequestParam(required = false) String to,
            @Parameter(description = "캔들 개수")
            @RequestParam(defaultValue = "200") int count) {
        List<Map<String, Object>> candles = upbitApiClient.getMinuteCandles(unit, market, to, count);
        return ResponseEntity.ok(candles);
    }
    
    @PostMapping("/test-connection")
    @Operation(summary = "업비트 API 연결 테스트", description = "API 키 없이 업비트 연결을 테스트합니다.")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            // 공개 API로 마켓 정보 조회를 통해 연결 테스트
            List<Map<String, Object>> markets = upbitApiClient.getMarkets();
            
            boolean isConnected = markets != null && !markets.isEmpty();
            
            Map<String, Object> response = Map.of(
                    "connected", isConnected,
                    "message", isConnected ? "업비트 API 연결 성공" : "업비트 API 연결 실패",
                    "marketCount", isConnected ? markets.size() : 0,
                    "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "connected", false,
                    "message", "업비트 API 연결 실패: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/test-auth")
    @Operation(summary = "업비트 API 키 테스트", description = "제공된 API 키로 인증을 테스트합니다.")
    public ResponseEntity<Map<String, Object>> testAuth(
            @Parameter(description = "업비트 Access Key" ,example = "AQhxPyb7kmBRmFrrzbWwLvXesdxD6Boq14KDx1Oc")
            @RequestParam String accessKey,
            @Parameter(description = "업비트 Secret Key",example = "KvzVi4heCSAVZnWNlxFYJNLLP1ixWgpskpYM7CRp")
            @RequestParam String secretKey) {
        try {
            // API 키 유효성 검증
            boolean isValid = upbitService.validateApiKeys(accessKey, secretKey);
            
            Map<String, Object> response = Map.of(
                    "valid", isValid,
                    "message", isValid ? "API 키가 유효합니다" : "API 키가 유효하지 않습니다",
                    "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "valid", false,
                    "message", "API 키 검증 중 오류 발생: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/debug/user-info")
    @Operation(summary = "사용자 정보 디버깅", description = "현재 인증된 사용자의 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> debugUserInfo(Authentication authentication) {
        try {
            Map<String, Object> debugInfo = upbitService.getDebugUserInfo(authentication);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "error", true,
                    "message", "사용자 정보 조회 실패: " + e.getMessage(),
                    "exception", e.getClass().getSimpleName(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/trades/ticks")
    @Operation(summary = "체결 내역 조회", description = "특정 마켓의 체결 내역을 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getTrades(
            @Parameter(description = "마켓 코드", example = "KRW-BTC")
            @RequestParam String market,
            @Parameter(description = "마지막 체결 시각")
            @RequestParam(required = false) String to,
            @Parameter(description = "체결 개수")
            @RequestParam(defaultValue = "200") int count,
            @Parameter(description = "페이지네이션 커서")
            @RequestParam(required = false) String cursor) {
        List<Map<String, Object>> trades = upbitApiClient.getTrades(market, to, count, cursor);
        return ResponseEntity.ok(trades);
    }
}