package com.autocoin.user.api;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestEntityConfig;
import com.autocoin.config.TestSchedulingConfig;
import com.autocoin.config.TestWebConfig;
import com.autocoin.config.TestJwtConfig;
import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.user.application.UserService;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.dto.UserLoginRequestDto;
import com.autocoin.user.dto.UserSignupRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 통합 테스트
 * 인증 관련 API 엔드포인트의 전체 플로우를 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestEntityConfig.class, TestSchedulingConfig.class, TestWebConfig.class, TestJwtConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private String testToken;
    private UserSignupRequestDto signupRequestDto;
    private UserLoginRequestDto loginRequestDto;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword")
                .username("testuser")
                .role(Role.USER)
                .build();

        // 테스트 JWT 토큰
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

        // 회원가입 요청 DTO
        signupRequestDto = UserSignupRequestDto.builder()
                .email("test@example.com")
                .password("Password123!")
                .username("testuser")
                .build();

        // 로그인 요청 DTO
        loginRequestDto = UserLoginRequestDto.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();
    }

    @Test
    @DisplayName("회원가입 API 성공 테스트")
    void signup_Success() throws Exception {
        // given
        when(userService.signup(any(UserSignupRequestDto.class))).thenReturn(testUser);

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("로그인 API 성공 테스트")
    void login_Success() throws Exception {
        // given
        List<String> roles = Collections.singletonList("ROLE_USER");
        when(userService.login(any(UserLoginRequestDto.class))).thenReturn(testUser);
        when(jwtTokenProvider.createToken(any(Long.class), any(String.class), any(List.class)))
                .thenReturn(testToken);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testToken))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    @DisplayName("내 정보 조회 API 성공 테스트")
    void getMe_Success() throws Exception {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        when(jwtTokenProvider.resolveToken(any())).thenReturn(testToken);
        when(jwtTokenProvider.validateToken(testToken)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(testToken)).thenReturn(auth);

        // when & then
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + testToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("내 정보 조회 API - 인증 토큰 없음")
    void getMe_NoToken() throws Exception {
        // given
        when(jwtTokenProvider.resolveToken(any())).thenReturn(null);

        // when & then
        mockMvc.perform(get("/api/v1/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 조회 API - 유효하지 않은 토큰")
    void getMe_InvalidToken() throws Exception {
        // given
        when(jwtTokenProvider.resolveToken(any())).thenReturn(testToken);
        when(jwtTokenProvider.validateToken(testToken)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + testToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("회원가입 API - 잘못된 입력값")
    void signup_InvalidInput() throws Exception {
        // given
        UserSignupRequestDto invalidRequest = UserSignupRequestDto.builder()
                .email("invalid-email")  // 잘못된 이메일 형식
                .password("123")         // 너무 짧은 비밀번호
                .username("")            // 빈 사용자명
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 API - 잘못된 입력값")
    void login_InvalidInput() throws Exception {
        // given
        UserLoginRequestDto invalidRequest = UserLoginRequestDto.builder()
                .email("")       // 빈 이메일
                .password("")    // 빈 비밀번호
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
