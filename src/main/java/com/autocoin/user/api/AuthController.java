package com.autocoin.user.api;

import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.user.application.UserService;
import com.autocoin.user.domain.User;
import com.autocoin.user.dto.UserLoginRequestDto;
import com.autocoin.user.dto.UserResponseDto;
import com.autocoin.user.dto.UserSignupRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.autocoin.global.exception.core.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "인증 API", description = "회원가입, 로그인, 사용자 정보 조회 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${server.port:5000}")
    private String serverPort;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 사용자 이름으로 새 계정을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "회원가입 성공",
                content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이메일 중복 또는 유효성 검사 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@Valid @RequestBody UserSignupRequestDto requestDto) {
        User user = userService.signup(requestDto);
        return new ResponseEntity<>(UserResponseDto.of(user), HttpStatus.CREATED);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 이메일 또는 비밀번호 불일치"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody UserLoginRequestDto requestDto) {
        User user = userService.login(requestDto);
        
        // Role 정보를 List<String>으로 변환
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), roles);
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", UserResponseDto.of(user));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "로그인 상태 확인", description = "인증 시스템의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상태 확인 성공",
                content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "내 정보 조회", description = "현재 인증된 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정보 조회 성공",
                content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 유효하지 않은 토큰"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .code("C001")
                            .message("Unauthorized")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
        return ResponseEntity.ok(UserResponseDto.of(user));
    }
    
    @Operation(summary = "Google OAuth2 로그인", description = "Google OAuth2 로그인 URL을 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Google OAuth2 로그인 URL 반환"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/oauth2/google")
    public ResponseEntity<Map<String, String>> getGoogleOAuth2Url() {
        String authUrl = "http://112.171.131.161:" + serverPort + "/oauth2/authorization/google";
        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Kakao OAuth2 로그인", description = "Kakao OAuth2 로그인 URL을 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kakao OAuth2 로그인 URL 반환"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/oauth2/kakao")
    public ResponseEntity<Map<String, String>> getKakaoOAuth2Url() {
        String authUrl = "http://112.171.131.161:" + serverPort + "/oauth2/authorization/kakao";
        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        return ResponseEntity.ok(response);
    }
}
