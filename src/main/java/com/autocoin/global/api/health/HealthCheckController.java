package com.autocoin.global.api.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

    // 루트 경로 처리
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> rootHealthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Autocoin API Server is running");
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> basicHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AutoCoin API");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            response.put("server_ip", localHost.getHostAddress());
            response.put("server_name", localHost.getHostName());
        } catch (UnknownHostException e) {
            response.put("server_ip", "Unknown");
        }
        
        Map<String, String> app = new HashMap<>();
        app.put("name", "autocoin-api");
        app.put("version", "1.0.0");
        response.put("application", app);
        
        return ResponseEntity.ok(response);
    }
}
