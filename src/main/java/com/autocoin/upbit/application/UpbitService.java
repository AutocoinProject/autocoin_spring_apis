package com.autocoin.upbit.application;

import com.autocoin.global.exception.core.CustomException;
import com.autocoin.global.exception.core.ErrorCode;
import com.autocoin.upbit.domain.UpbitAccountRepository;
import com.autocoin.upbit.domain.entity.UpbitAccount;
import com.autocoin.upbit.dto.UpbitAccountInfoDto;
import com.autocoin.upbit.dto.UpbitTickerDto;
import com.autocoin.upbit.dto.request.UpbitConnectRequestDto;
import com.autocoin.upbit.dto.response.UpbitAccountStatusResponseDto;
import com.autocoin.upbit.dto.response.UpbitConnectResponseDto;
import com.autocoin.upbit.dto.response.WalletResponseDto;
import com.autocoin.upbit.infrastructure.UpbitApiClient;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UpbitService {
    
    private final UpbitAccountRepository upbitAccountRepository;
    private final UserRepository userRepository;
    private final UpbitApiClient upbitApiClient;
    private final UpbitCryptoService upbitCryptoService;
    private final UpbitAuthService upbitAuthService;
    
    /**
     * API 키 유효성 검증 (공개 메서드)
     */
    public boolean validateApiKeys(String accessKey, String secretKey) {
        try {
            return upbitAuthService.validateApiKeys(accessKey, secretKey);
        } catch (Exception e) {
            log.error("API 키 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 디버깅용 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDebugUserInfo(Authentication authentication) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            // Authentication 객체 정보
            debugInfo.put("authentication_name", authentication != null ? authentication.getName() : null);
            debugInfo.put("authentication_class", authentication != null ? authentication.getClass().getSimpleName() : null);
            debugInfo.put("principal_class", authentication != null && authentication.getPrincipal() != null ? 
                    authentication.getPrincipal().getClass().getSimpleName() : null);
            
            if (authentication != null) {
                String userEmail = authentication.getName();
                debugInfo.put("extracted_email", userEmail);
                
                // 데이터베이스에서 사용자 조회 시도
                Optional<User> userOpt = userRepository.findByEmail(userEmail);
                debugInfo.put("user_found", userOpt.isPresent());
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    debugInfo.put("user_id", user.getId());
                    debugInfo.put("user_email", user.getEmail());
                    debugInfo.put("user_username", user.getUsername());
                    debugInfo.put("user_role", user.getRole());
                    debugInfo.put("user_provider", user.getProvider());
                    debugInfo.put("user_created_at", user.getCreatedAt());
                } else {
                    debugInfo.put("error_message", "User not found in database");
                    
                    // 전체 사용자 수 정보 (대체 방법)
                    debugInfo.put("total_users_note", "User findAll method not available - need manual check");
                    
                    // ID 1로 사용자 조회 시도 (JWT에서 userId=1이었음)
                    Optional<User> userById1 = userRepository.findById(1L);
                    if (userById1.isPresent()) {
                        User user1 = userById1.get();
                        debugInfo.put("user_id_1_email", user1.getEmail());
                        debugInfo.put("user_id_1_username", user1.getUsername());
                    } else {
                        debugInfo.put("user_id_1_found", false);
                    }
                }
            } else {
                debugInfo.put("error_message", "Authentication is null");
            }
            
        } catch (Exception e) {
            debugInfo.put("exception", e.getMessage());
            debugInfo.put("exception_class", e.getClass().getSimpleName());
            log.error("디버깅 정보 수집 실패: {}", e.getMessage(), e);
        }
        
        debugInfo.put("timestamp", System.currentTimeMillis());
        return debugInfo;
    }
    
    /**
     * 업비트 계정 연결
     */
    public UpbitConnectResponseDto connectUpbitAccount(UpbitConnectRequestDto request, Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        try {
            // API 키 유효성 검증
            boolean isValid = upbitAuthService.validateApiKeys(request.getAccessKey(), request.getSecretKey());
            
            if (!isValid) {
                throw new CustomException(ErrorCode.INVALID_UPBIT_API_KEYS);
            }
            
            // API 키 암호화
            String encryptedAccessKey = upbitCryptoService.encrypt(request.getAccessKey());
            String encryptedSecretKey = upbitCryptoService.encrypt(request.getSecretKey());
            
            // 기존 계정 확인
            Optional<UpbitAccount> existingAccount = upbitAccountRepository.findByUser(user);
            
            UpbitAccount upbitAccount;
            if (existingAccount.isPresent()) {
                upbitAccount = existingAccount.get();
                upbitAccount.updateApiKeys(encryptedAccessKey, encryptedSecretKey);
                if (request.getNickname() != null) {
                    upbitAccount.updateNickname(request.getNickname());
                }
            } else {
                upbitAccount = UpbitAccount.builder()
                        .user(user)
                        .encryptedAccessKey(encryptedAccessKey)
                        .encryptedSecretKey(encryptedSecretKey)
                        .accountState(UpbitAccount.AccountState.ACTIVE)
                        .lastSyncAt(LocalDateTime.now())
                        .nickname(request.getNickname())
                        .build();
            }
            
            upbitAccountRepository.save(upbitAccount);
            
            return UpbitConnectResponseDto.builder()
                    .success(true)
                    .message("업비트 계정이 성공적으로 연결되었습니다.")
                    .accountState(upbitAccount.getAccountState().name())
                    .nickname(upbitAccount.getNickname())
                    .build();
                    
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 계정 연결 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.UPBIT_CONNECTION_FAILED);
        }
    }
    
    /**
     * 업비트 계정 상태 조회
     */
    @Transactional(readOnly = true)
    public UpbitAccountStatusResponseDto getUpbitAccountStatus(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        Optional<UpbitAccount> upbitAccount = upbitAccountRepository.findByUser(user);
        
        if (upbitAccount.isPresent()) {
            UpbitAccount account = upbitAccount.get();
            return UpbitAccountStatusResponseDto.builder()
                    .connected(true)
                    .accountState(account.getAccountState().name())
                    .lastSyncAt(account.getLastSyncAt())
                    .nickname(account.getNickname())
                    .build();
        } else {
            return UpbitAccountStatusResponseDto.builder()
                    .connected(false)
                    .accountState("NONE")
                    .build();
        }
    }
    
    /**
     * 지갑 정보 조회
     */
    @Transactional(readOnly = true)
    public List<WalletResponseDto> getWalletInfo(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        UpbitAccount upbitAccount = upbitAccountRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.UPBIT_ACCOUNT_NOT_FOUND));
        
        if (!upbitAccount.isActive()) {
            throw new CustomException(ErrorCode.UPBIT_ACCOUNT_INACTIVE);
        }
        
        try {
            // API 키 복호화
            String accessKey = upbitCryptoService.decrypt(upbitAccount.getEncryptedAccessKey());
            String secretKey = upbitCryptoService.decrypt(upbitAccount.getEncryptedSecretKey());
            
            // 업비트 API 호출
            List<UpbitAccountInfoDto> accounts = upbitApiClient.getAccounts(accessKey, secretKey);
            
            // 응답 DTO 변환
            return accounts.stream()
                    .map(account -> WalletResponseDto.builder()
                            .currency(account.getCurrency())
                            .balance(account.getBalance())
                            .locked(account.getLocked())
                            .avgBuyPrice(account.getAvgBuyPrice())
                            .avgBuyPriceModified(account.isAvgBuyPriceModified())
                            .unitCurrency(account.getUnitCurrency())
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("지갑 정보 조회 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.UPBIT_API_ERROR);
        }
    }
    
    /**
     * 암호화폐 시세 조회
     */
    @Transactional(readOnly = true)
    public List<UpbitTickerDto> getMarketTickers(List<String> markets) {
        try {
            return upbitApiClient.getTickers(markets);
        } catch (Exception e) {
            log.error("시세 정보 조회 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.UPBIT_API_ERROR);
        }
    }
    
    /**
     * 업비트 계정 연결 해제
     */
    public void disconnectUpbitAccount(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        UpbitAccount upbitAccount = upbitAccountRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.UPBIT_ACCOUNT_NOT_FOUND));
        
        upbitAccount.deactivate();
        upbitAccountRepository.save(upbitAccount);
        
        log.info("사용자 {}의 업비트 계정 연결이 해제되었습니다.", userEmail);
    }
    
    /**
     * 계정 상태 동기화
     */
    @Transactional
    public void syncAccountStatus(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("[계정 동기화] 시작 - 요청 사용자 이메일: {}", userEmail);
        
        // Authentication 객체 상세 정보 로깅
        if (authentication != null && authentication.getPrincipal() != null) {
            log.info("[계정 동기화] Authentication 정보 - Principal 타입: {}, 이름: {}", 
                    authentication.getPrincipal().getClass().getSimpleName(), authentication.getName());
        }
        
        // 사용자 조회 전 디버깅
        log.debug("[계정 동기화] 데이터베이스에서 사용자 조회 시도: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("[계정 동기화] 사용자를 찾을 수 없음: {}", userEmail);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });
        
        log.info("[계정 동기화] 사용자 조회 성공 - ID: {}, 이메일: {}, 사용자명: {}", 
                user.getId(), user.getEmail(), user.getUsername());
        
        // 업비트 계정 조회 - 없는 경우 예외 대신 적절한 응답 반환
        Optional<UpbitAccount> upbitAccountOpt = upbitAccountRepository.findByUser(user);
        
        if (upbitAccountOpt.isEmpty()) {
            log.warn("[계정 동기화] 업비트 계정이 연결되지 않음 - 사용자 ID: {}, 이메일: {}", user.getId(), userEmail);
            throw new CustomException(ErrorCode.UPBIT_ACCOUNT_NOT_FOUND);
        }
        
        UpbitAccount upbitAccount = upbitAccountOpt.get();
        log.info("[계정 동기화] 업비트 계정 조회 성공 - 계정 상태: {}, 마지막 동기화: {}", 
                upbitAccount.getAccountState(), upbitAccount.getLastSyncAt());
        
        try {
            // API 키 복호화
            String accessKey = upbitCryptoService.decrypt(upbitAccount.getEncryptedAccessKey());
            String secretKey = upbitCryptoService.decrypt(upbitAccount.getEncryptedSecretKey());
            
            // API 키 유효성 재검증
            boolean isValid = upbitAuthService.validateApiKeys(accessKey, secretKey);
            
            if (isValid) {
                upbitAccount.updateAccountState(UpbitAccount.AccountState.ACTIVE);
                log.info("[계정 동기화] API 키 검증 성공 - 계정 상태를 ACTIVE로 업데이트");
            } else {
                upbitAccount.updateAccountState(UpbitAccount.AccountState.ERROR);
                log.warn("[계정 동기화] API 키 검증 실패 - 계정 상태를 ERROR로 업데이트");
            }
            
            upbitAccountRepository.save(upbitAccount);
            
            log.info("[계정 동기화] 완료 - 최종 계정 상태: {}", upbitAccount.getAccountState());
            
        } catch (Exception e) {
            log.error("[계정 동기화] 계정 상태 동기화 실패: {}", e.getMessage(), e);
            upbitAccount.updateAccountState(UpbitAccount.AccountState.ERROR);
            upbitAccountRepository.save(upbitAccount);
            throw new CustomException(ErrorCode.UPBIT_SYNC_FAILED);
        }
    }
}