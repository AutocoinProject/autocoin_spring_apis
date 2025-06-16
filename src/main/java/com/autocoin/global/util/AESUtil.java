package com.autocoin.global.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AESUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // GCM 권장 IV 길이
    private static final int TAG_LENGTH = 16; // GCM 태그 길이
    
    /**
     * 비밀키를 32바이트로 정규화
     */
    private SecretKeySpec getSecretKeySpec(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        
        // 키를 정확히 32바이트로 맞춤
        byte[] normalizedKey = new byte[32];
        if (keyBytes.length >= 32) {
            System.arraycopy(keyBytes, 0, normalizedKey, 0, 32);
        } else {
            System.arraycopy(keyBytes, 0, normalizedKey, 0, keyBytes.length);
            // 부족한 부분은 0으로 패딩
            for (int i = keyBytes.length; i < 32; i++) {
                normalizedKey[i] = 0;
            }
        }
        
        return new SecretKeySpec(normalizedKey, ALGORITHM);
    }
    
    /**
     * 문자열 암호화
     */
    public String encrypt(String plainText, String secretKey) throws Exception {
        SecretKeySpec keySpec = getSecretKeySpec(secretKey);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // 랜덤 IV 생성
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        // IV + encrypted 데이터를 결합하여 Base64 인코딩
        byte[] encryptedWithIv = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, encryptedWithIv, IV_LENGTH, encrypted.length);
        
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }
    
    /**
     * 문자열 복호화
     */
    public String decrypt(String encryptedText, String secretKey) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedText);
        
        // IV와 암호화된 데이터 분리
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[decodedData.length - IV_LENGTH];
        
        System.arraycopy(decodedData, 0, iv, 0, IV_LENGTH);
        System.arraycopy(decodedData, IV_LENGTH, encrypted, 0, encrypted.length);
        
        SecretKeySpec keySpec = getSecretKeySpec(secretKey);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}