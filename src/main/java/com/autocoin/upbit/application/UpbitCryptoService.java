package com.autocoin.upbit.application;

import com.autocoin.global.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpbitCryptoService {
    
    @Value("${app.upbit.encryption.key:autocoin-upbit-encrypt-key-32ch0}")
    private String encryptionKey;
    
    private final AESUtil aesUtil;
    
    /**
     * 업비트 API 키 암호화
     */
    public String encrypt(String plainText) {
        try {
            return aesUtil.encrypt(plainText, encryptionKey);
        } catch (Exception e) {
            log.error("업비트 API 키 암호화 실패", e);
            throw new RuntimeException("암호화 실패", e);
        }
    }
    
    /**
     * 업비트 API 키 복호화
     */
    public String decrypt(String encryptedText) {
        try {
            return aesUtil.decrypt(encryptedText, encryptionKey);
        } catch (Exception e) {
            log.error("업비트 API 키 복호화 실패", e);
            throw new RuntimeException("복호화 실패", e);
        }
    }
}