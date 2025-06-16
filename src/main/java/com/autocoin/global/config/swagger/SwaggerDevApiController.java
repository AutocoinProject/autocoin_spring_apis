package com.autocoin.global.config.swagger;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.user.application.UserService;
import com.autocoin.user.domain.User;
import com.autocoin.user.dto.UserLoginRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Swagger UI 테스트를 위한 개발 지원 API 컨트롤러
 * local 프로필에서만 활성화됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/swagger-dev")
@Profile("local")
@RequiredArgsConstructor
public class SwaggerDevApiController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Swagger UI 테스트를 위한 토큰 검증 API
     * 토큰의 유효성을 검사하고 상세 정보를 반환합니다.
     */
    @GetMapping("/token/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isValid = jwtTokenProvider.validateToken(token);
            result.put("valid", isValid);
            
            if (isValid) {
                // 토큰에서 정보 추출
                Long userId = jwtTokenProvider.getUserId(token);
                String email = jwtTokenProvider.getEmail(token);
                
                // 응답 데이터 생성
                result.put("userId", userId);
                result.put("email", email);
                
                // 토큰 만료 시간 계산
                long expirationTimeInSeconds = jwtTokenProvider.getExpirationTime(token);
                result.put("expiresIn", expirationTimeInSeconds);
                result.put("expiresInMinutes", Math.round(expirationTimeInSeconds / 60.0));
            }
        } catch (Exception e) {
            log.debug("토큰 검증 오류: {}", e.getMessage());
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Swagger UI 테스트를 위한 간편 로그인 API
     * 테스트 계정으로 자동 로그인하여 토큰을 발급합니다.
     */
    @PostMapping("/test-login")
    public ResponseEntity<Map<String, Object>> testLogin() {
        try {
            // 테스트 계정 정보
            UserLoginRequestDto loginRequest = UserLoginRequestDto.builder()
                    .email("test@autocoin.com")
                    .password("Test1234!")
                    .build();
            
            // 로그인 시도
            User user = userService.login(loginRequest);
            
            // Role 정보를 List<String>으로 변환
            List<String> roles = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            // JWT 토큰 생성
            String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), roles);
            
            // 응답 데이터 생성
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("테스트 로그인 오류: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "로그인 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
