package com.autocoin.user.application;

import com.autocoin.global.exception.core.CustomException;
import com.autocoin.global.exception.core.ErrorCode;
import com.autocoin.global.util.PasswordEncoderUtil;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import com.autocoin.user.dto.UserLoginRequestDto;
import com.autocoin.user.dto.UserSignupRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * UserService 클래스의 단위 테스트
 * 
 * 이 테스트 클래스는 UserService의 다음 기능을 검증합니다:
 * 1. 회원가입 - 성공 및 이메일 중복 시 실패 케이스
 * 2. 로그인 - 성공 및 사용자 없음/비밀번호 불일치 시 실패 케이스
 * 3. 사용자 조회 - ID와 이메일 기준 조회 성공 및 실패 케이스
 * 
 * 모든 테스트는 Mock 객체를 사용하여 외부 의존성(repository, encoder)을 격리하고,
 * Given-When-Then 패턴을 따라 작성되었습니다.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

    @InjectMocks
    private UserService userService;

    private UserSignupRequestDto signupRequestDto;
    private UserLoginRequestDto loginRequestDto;
    private User user;

    /**
     * 각 테스트 전에 실행되는 설정 메서드
     * 테스트에 필요한 DTO와 엔티티 객체를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        // 회원가입 요청 DTO 설정
        signupRequestDto = UserSignupRequestDto.builder()
                .email("test@example.com")
                .password("Password123!")
                .username("testuser")
                .build();

        // 로그인 요청 DTO 설정
        loginRequestDto = UserLoginRequestDto.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        // 테스트용 사용자 객체 설정
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .username("testuser")
                .role(Role.USER)
                .build();
    }

    /**
     * 회원가입 성공 테스트
     * 
     * 검증 내용:
     * - 이메일 중복이 없을 때 회원가입이 성공적으로 진행되는지 확인
     * - 결과로 반환된 User 객체의 정보가 요청 DTO와 일치하는지 확인
     */
    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_Success() {
        // Given: 이메일 중복이 없고, 비밀번호 인코딩과 사용자 저장이 성공적으로 이루어지는 상황
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoderUtil.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        // When: 회원가입 메서드를 호출
        User result = userService.signup(signupRequestDto);

        // Then: 반환된 사용자 정보가 예상과 일치하는지 검증
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testuser", result.getUsername());
        assertEquals(Role.USER, result.getRole());
    }

    /**
     * 회원가입 실패 테스트 - 이메일 중복
     * 
     * 검증 내용:
     * - 이미 동일한 이메일이 존재할 경우 예외가 발생하는지 확인
     * - 발생한 예외의 에러 코드가 EMAIL_DUPLICATION인지 확인
     */
    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 중복")
    void signup_Failure_EmailDuplication() {
        // Given: 이미 동일한 이메일이 존재하는 상황
        given(userRepository.existsByEmail(anyString())).willReturn(true);

        // When & Then: 회원가입 메서드 호출 시 예외가 발생하고, 예외 정보가 예상과 일치하는지 검증
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.signup(signupRequestDto));
        assertEquals(ErrorCode.EMAIL_DUPLICATION, exception.getErrorCode());
    }

    /**
     * 로그인 성공 테스트
     * 
     * 검증 내용:
     * - 사용자가 존재하고 비밀번호가 일치할 때 로그인이 성공하는지 확인
     * - 반환된 User 객체의 정보가 예상과 일치하는지 확인
     */
    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // Given: 사용자가 존재하고 비밀번호가 일치하는 상황
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoderUtil.matches(anyString(), anyString())).willReturn(true);

        // When: 로그인 메서드를 호출
        User result = userService.login(loginRequestDto);

        // Then: 반환된 사용자 정보가 예상과 일치하는지 검증
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testuser", result.getUsername());
    }

    /**
     * 로그인 실패 테스트 - 사용자 없음
     * 
     * 검증 내용:
     * - 이메일에 해당하는 사용자가 없을 때 예외가 발생하는지 확인
     * - 발생한 예외의 에러 코드가 LOGIN_FAILED인지 확인
     */
    @Test
    @DisplayName("로그인 실패 테스트 - 사용자 없음")
    void login_Failure_UserNotFound() {
        // Given: 이메일에 해당하는 사용자가 없는 상황
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // When & Then: 로그인 메서드 호출 시 예외가 발생하고, 예외 정보가 예상과 일치하는지 검증
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.login(loginRequestDto));
        assertEquals(ErrorCode.EMAIL_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 로그인 실패 테스트 - 비밀번호 불일치
     * 
     * 검증 내용:
     * - 사용자는 존재하지만 비밀번호가 일치하지 않을 때 예외가 발생하는지 확인
     * - 발생한 예외의 에러 코드가 LOGIN_FAILED인지 확인
     */
    @Test
    @DisplayName("로그인 실패 테스트 - 비밀번호 불일치")
    void login_Failure_PasswordMismatch() {
        // Given: 사용자는 존재하지만 비밀번호가 일치하지 않는 상황
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoderUtil.matches(anyString(), anyString())).willReturn(false);

        // When & Then: 로그인 메서드 호출 시 예외가 발생하고, 예외 정보가 예상과 일치하는지 검증
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.login(loginRequestDto));
        assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }

    /**
     * 사용자 ID로 조회 성공 테스트
     * 
     * 검증 내용:
     * - ID에 해당하는 사용자가 존재할 때 조회가 성공하는지 확인
     * - 반환된 User 객체의 정보가 예상과 일치하는지 확인
     */
    @Test
    @DisplayName("사용자 ID로 조회 테스트")
    void findUserById_Success() {
        // Given: ID에 해당하는 사용자가 존재하는 상황
        given(userRepository.findById(any(Long.class))).willReturn(Optional.of(user));

        // When: ID로 사용자 조회 메서드를 호출
        User result = userService.findUserById(1L);

        // Then: 반환된 사용자 정보가 예상과 일치하는지 검증
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    /**
     * 사용자 ID로 조회 실패 테스트
     * 
     * 검증 내용:
     * - ID에 해당하는 사용자가 없을 때 예외가 발생하는지 확인
     * - 발생한 예외의 에러 코드가 USER_NOT_FOUND인지 확인
     */
    @Test
    @DisplayName("사용자 ID로 조회 실패 테스트")
    void findUserById_Failure() {
        // Given: ID에 해당하는 사용자가 없는 상황
        given(userRepository.findById(any(Long.class))).willReturn(Optional.empty());

        // When & Then: ID로 사용자 조회 메서드 호출 시 예외가 발생하고, 예외 정보가 예상과 일치하는지 검증
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.findUserById(1L));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 이메일로 사용자 조회 성공 테스트
     * 
     * 검증 내용:
     * - 이메일에 해당하는 사용자가 존재할 때 조회가 성공하는지 확인
     * - 반환된 User 객체의 정보가 예상과 일치하는지 확인
     */
    @Test
    @DisplayName("이메일로 사용자 조회 테스트")
    void findUserByEmail_Success() {
        // Given: 이메일에 해당하는 사용자가 존재하는 상황
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // When: 이메일로 사용자 조회 메서드를 호출
        User result = userService.findUserByEmail("test@example.com");

        // Then: 반환된 사용자 정보가 예상과 일치하는지 검증
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    /**
     * 이메일로 사용자 조회 실패 테스트
     * 
     * 검증 내용:
     * - 이메일에 해당하는 사용자가 없을 때 예외가 발생하는지 확인
     * - 발생한 예외의 에러 코드가 USER_NOT_FOUND인지 확인
     */
    @Test
    @DisplayName("이메일로 사용자 조회 실패 테스트")
    void findUserByEmail_Failure() {
        // Given: 이메일에 해당하는 사용자가 없는 상황
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // When & Then: 이메일로 사용자 조회 메서드 호출 시 예외가 발생하고, 예외 정보가 예상과 일치하는지 검증
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.findUserByEmail("test@example.com"));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
