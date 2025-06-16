package com.autocoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutocoinSpringApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutocoinSpringApiApplication.class, args);
    }
    
    // CORS 설정을 SecurityConfig로 통합하여 중복 제거
}
