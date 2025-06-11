package com.autocoin.global.auth.util;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Request의 Header에서 JWT 토큰 추출
     */
    public String getToken(HttpServletRequest request) {
        return jwtTokenProvider.resolveToken(request);
    }
    
    /**
     * JWT 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmail(token);
    }
    
    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
}
