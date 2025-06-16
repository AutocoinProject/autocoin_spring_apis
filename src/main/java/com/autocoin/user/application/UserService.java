package com.autocoin.user.application;

import com.autocoin.global.exception.core.CustomException;
import com.autocoin.global.exception.core.ErrorCode;
import com.autocoin.global.util.PasswordEncoderUtil;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import com.autocoin.user.dto.UserLoginRequestDto;
import com.autocoin.user.dto.UserSignupRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoderUtil passwordEncoderUtil;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일로 사용자 정보 조회, 없을 경우 예외 발생
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User signup(UserSignupRequestDto requestDto) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATION);
        }

        // bcrypt로 암호화된 비밀번호로 새 사용자 생성
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoderUtil.encode(requestDto.getPassword()))
                .username(requestDto.getUsername())
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public User login(UserLoginRequestDto requestDto) {
        String email = requestDto.getEmail();
        log.info("로그인 시도: {}", email);
        
        // 이메일로 사용자 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: {}", email);
                    return new CustomException(ErrorCode.EMAIL_NOT_FOUND);
                });
        
        log.info("사용자 발견: {} (ID: {})", email, user.getId());
        
        // bcrypt를 사용해 비밀번호 검증
        boolean passwordMatches = passwordEncoderUtil.matches(requestDto.getPassword(), user.getPassword());
        log.debug("비밀번호 매치 결과: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.warn("비밀번호 불일치: {}", email);
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }
        
        log.info("로그인 성공: {}", email);
        return user;
    }

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        // ID로 사용자 찾기, 없을 경우 예외 발생
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        // 이메일로 사용자 찾기, 없을 경우 예외 발생
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
