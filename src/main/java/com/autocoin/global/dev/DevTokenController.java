package com.autocoin.global.dev;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 개발용 토큰 관리 컨트롤러
 * 로컬 환경에서만 활성화됩니다.
 */
@RestController
@RequestMapping("/dev/token")
@Profile("local")
@RequiredArgsConstructor
public class DevTokenController {

    private final DevTokenService devTokenService;

    /**
     * 개발용 장기 토큰 생성 (1년 유효)
     */
    @GetMapping("/long-term")
    public ResponseEntity<Map<String, Object>> generateLongTermToken() {
        String token = devTokenService.generateLongTermDevToken();
        DevUserInfo userInfo = devTokenService.getDevUserInfo();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "long-term");
        response.put("expires", "1 year");
        response.put("user", userInfo);
        response.put("usage", "개발 환경에서 1년간 사용 가능한 토큰");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 개발용 단기 토큰 생성 (30분 유효)
     */
    @GetMapping("/short-term")
    public ResponseEntity<Map<String, Object>> generateShortTermToken() {
        String token = devTokenService.generateShortTermDevToken();
        DevUserInfo userInfo = devTokenService.getDevUserInfo();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "short-term");
        response.put("expires", "30 minutes");
        response.put("user", userInfo);
        response.put("usage", "개발 환경에서 30분간 사용 가능한 토큰");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 개발용 사용자 정보 조회
     */
    @GetMapping("/user-info")
    public ResponseEntity<DevUserInfo> getDevUserInfo() {
        return ResponseEntity.ok(devTokenService.getDevUserInfo());
    }
}
