package com.autocoin.integration;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestEntityConfig;
import com.autocoin.config.TestSchedulingConfig;
import com.autocoin.config.TestWebConfig;
import com.autocoin.config.TestJwtConfig;
import com.autocoin.global.util.PasswordEncoderUtil;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.infrastructure.UserJpaRepository; // JpaRepository 사용
import com.autocoin.user.dto.UserLoginRequestDto;
import com.autocoin.user.dto.UserSignupRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT 인증 시스템 통합 테스트
 * 실제 데이터베이스와 전체 Spring 컨텍스트를 사용한 end-to-end 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestEntityConfig.class, TestSchedulingConfig.class, TestWebConfig.class, TestJwtConfig.class})
@Transactional
@TestMethodOrder(OrderAnnotation.class)
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository; // JpaRepository 사용

    @Autowired
    private PasswordEncoderUtil passwordEncoderUtil;

    private UserSignupRequestDto signupRequestDto;
    private UserLoginRequestDto loginRequestDto;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화 - JpaRepository의 deleteAll() 사용
        userJpaRepository.deleteAll();

        // 회원가입 요청 DTO
        signupRequestDto = UserSignupRequestDto.builder()
                .email("integrationtest@example.com")
                .password("IntegrationTest123!")
                .username("testuser1") // 오타 수정: tester -> testuser1
                .build();

        // 로그인 요청 DTO
        loginRequestDto = UserLoginRequestDto.builder()
                .email("integrationtest@example.com")
                .password("IntegrationTest123!")
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("회원가입-로그인-내정보조회 전체 플로우 테스트")
    void fullAuthenticationFlow() throws Exception {
        // 1. 회원가입
        ResultActions signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

        signupResult.andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integrationtest@example.com"))
                .andExpect(jsonPath("$.username").value("testuser1")); // 오타 수정

        // 2. 로그인
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDto)));

        MvcResult loginMvcResult = loginResult.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("testuser1")) // 오타 수정
                .andExpect(jsonPath("$.user.email").value("integrationtest@example.com"))
                .andReturn();

        // 토큰 추출 - JsonNode 사용으로 안전한 파싱 및 타입 안전성 보장
        String responseContent = loginMvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String token = jsonNode.get("token").asText();

        assertNotNull(token, "토큰은 null이 아니어야 합니다");
        assertFalse(token.isEmpty(), "토큰은 빈 문자열이 아니어야 합니다"); // isEmpty() 사용

        // 3. 내 정보 조회
        ResultActions meResult = mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));

        meResult.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integrationtest@example.com"))
                .andExpect(jsonPath("$.username").value("testuser1")); // 오타 수정
    }

    @Test
    @Order(2)
    @DisplayName("잘못된 비밀번호로 로그인 실패 테스트")
    void loginFailure_WrongPassword() throws Exception {
        // 사용자 미리 생성
        User user = User.builder()
                .email("wrongpassword@example.com")
                .password(passwordEncoderUtil.encode("CorrectPassword123!"))
                .username("wronguser1") // 오타 수정: wrongpass -> wronguser1
                .role(Role.USER)
                .build();
        userJpaRepository.save(user);

        // 잘못된 비밀번호로 로그인 시도
        UserLoginRequestDto wrongPasswordRequest = UserLoginRequestDto.builder()
                .email("wrongpassword@example.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("존재하지 않는 이메일로 로그인 실패 테스트")
    void loginFailure_NonExistentEmail() throws Exception {
        UserLoginRequestDto nonExistentRequest = UserLoginRequestDto.builder()
                .email("nonexistent@example.com")
                .password("SomePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("인증 없이 보호된 경로 접근 실패 테스트")
    void accessProtectedRouteWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("유효하지 않은 토큰으로 접근 실패 테스트")
    void accessWithInvalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("만료된 토큰으로 접근 실패 테스트")
    void accessWithExpiredToken() throws Exception {
        // 만료된 토큰 시뮬레이션 (실제로는 토큰 생성 시 짧은 만료 시간 설정)
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("회원가입 시 이메일 중복 검증 테스트")
    void signupFailure_DuplicateEmail() throws Exception {
        // 첫 번째 회원가입 성공
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isCreated());

        // 같은 이메일로 두 번째 회원가입 시도 (실패)
        UserSignupRequestDto duplicateEmailRequest = UserSignupRequestDto.builder()
                .email("integrationtest@example.com") // 중복 이메일
                .password("DifferentPassword123!")
                .username("differentuser1") // 오타 수정: differentuser -> differentuser1
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    @DisplayName("회원가입 시 사용자명 중복 검증 테스트")
    void signupFailure_DuplicateUsername() throws Exception {
        // 첫 번째 회원가입 성공
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isCreated());

        // 같은 사용자명으로 두 번째 회원가입 시도 (실패)
        UserSignupRequestDto duplicateUsernameRequest = UserSignupRequestDto.builder()
                .email("different@example.com")
                .password("DifferentPassword123!")
                .username("testuser1") // 중복 사용자명
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUsernameRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(9)
    @DisplayName("회원가입 시 약한 비밀번호 검증 테스트")
    void signupFailure_WeakPassword() throws Exception {
        UserSignupRequestDto weakPasswordRequest = UserSignupRequestDto.builder()
                .email("weakpassword@example.com")
                .password("123") // 약한 비밀번호
                .username("weakuser1") // 오타 수정: weakuser -> weakuser1
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("회원가입 시 잘못된 이메일 형식 검증 테스트")
    void signupFailure_InvalidEmailFormat() throws Exception {
        UserSignupRequestDto invalidEmailRequest = UserSignupRequestDto.builder()
                .email("invalid-email-format") // 잘못된 이메일 형식
                .password("ValidPassword123!")
                .username("validuser1") // 오타 수정: validuser -> validuser1
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("로그인 후 토큰으로 여러 API 호출 테스트")
    void multipleApiCallsWithToken() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isCreated());

        // 2. 로그인하여 토큰 획득
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String token = jsonNode.get("token").asText();

        // 3. 토큰으로 여러 API 호출
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("integrationtest@example.com"));
        }
    }
}
