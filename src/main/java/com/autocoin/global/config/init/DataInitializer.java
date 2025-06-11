package com.autocoin.global.config.init;

import com.autocoin.category.application.service.CategoryService;
import com.autocoin.global.util.PasswordEncoderUtil;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 필요한 기본 데이터를 초기화하는 컴포넌트
 * app.init.enabled=true 설정 시에만 실행됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.init.enabled", havingValue = "true", matchIfMissing = false)
public class DataInitializer {

    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final PasswordEncoderUtil passwordEncoderUtil;

    /**
     * 애플리케이션 시작 시 기본 데이터 초기화
     */
    @PostConstruct
    @Transactional
    public void init() {
        log.info("데이터 초기화 시작");
        initCategories();
        // initTestUsers();  // 테스트 사용자 자동 생성 비활성화
        log.info("데이터 초기화 완료");
    }

    /**
     * 기본 카테고리 초기화
     */
    private void initCategories() {
        log.info("카테고리 초기화 시작");
        categoryService.initDefaultCategories();
        log.info("카테고리 초기화 완료");
    }
    
    /**
     * 테스트 사용자 초기화 - 비활성화됨
     */
    /*
    private void initTestUsers() {
        log.info("테스트 사용자 초기화 시작");
        
        // 테스트 사용자가 이미 존재하는지 확인
        if (!userRepository.existsByEmail("test@autocoin.com")) {
            User testUser = User.builder()
                .email("test@autocoin.com")
                .username("testuser")
                .password(passwordEncoderUtil.encode("test123")) // 비밀번호: test123
                .role(Role.ROLE_USER)
                .build();
            
            userRepository.save(testUser);
            log.info("테스트 사용자 생성 완료: test@autocoin.com (비밀번호: test123)");
        } else {
            log.info("테스트 사용자가 이미 존재합니다: test@autocoin.com");
        }
        
        // 관리자 사용자 생성
        if (!userRepository.existsByEmail("admin@autocoin.com")) {
            User adminUser = User.builder()
                .email("admin@autocoin.com")
                .username("admin")
                .password(passwordEncoderUtil.encode("admin123")) // 비밀번호: admin123
                .role(Role.ROLE_ADMIN)
                .build();
            
            userRepository.save(adminUser);
            log.info("관리자 사용자 생성 완료: admin@autocoin.com (비밀번호: admin123)");
        } else {
            log.info("관리자 사용자가 이미 존재합니다: admin@autocoin.com");
        }
        
        log.info("테스트 사용자 초기화 완료");
    }
    */
}
