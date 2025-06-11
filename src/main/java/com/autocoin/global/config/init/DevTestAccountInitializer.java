package com.autocoin.global.config.init;

import com.autocoin.user.application.UserService;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import com.autocoin.user.dto.UserSignupRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 개발 환경에서 사용할 테스트 계정을 자동으로 생성합니다.
 * local 프로필에서만 활성화됩니다.
 * 현재 비활성화됨 - 수동으로 계정 생성 필요
 */
@Slf4j
// @Component  // 자동 테스트 계정 생성 비활성화
@RequiredArgsConstructor
@Profile("local")
@Order(1)
public class DevTestAccountInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        createTestAccount();
    }

    /**
     * 테스트 계정을 생성합니다.
     */
    private void createTestAccount() {
        String testEmail = "test@autocoin.com";
        
        if (!userRepository.existsByEmail(testEmail)) {
            log.info("테스트 계정 생성 시작: {}", testEmail);
            try {
                UserSignupRequestDto requestDto = UserSignupRequestDto.builder()
                        .email(testEmail)
                        .password("Test1234!")
                        .username("테스트 사용자")
                        .build();
                
                User testUser = userService.signup(requestDto);
                log.info("테스트 계정 생성 완료: {}, ID: {}", testEmail, testUser.getId());
                log.info("Swagger UI에서 테스트 계정으로 자동 로그인할 수 있습니다.");
                log.info("테스트 계정 정보 - 이메일: {}, 비밀번호: Test1234!", testEmail);
            } catch (Exception e) {
                log.error("테스트 계정 생성 실패: {}", e.getMessage());
            }
        } else {
            log.info("테스트 계정이 이미 존재합니다: {} (비밀번호: Test1234!)", testEmail);
        }
    }
}
