package com.autocoin.user.dto;

import com.autocoin.user.domain.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 응답 DTO")
public class UserResponseDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    
    @Schema(description = "사용자 이메일", example = "test@autocoin.com")
    private String email;
    
    @Schema(description = "사용자 이름", example = "테스트사용자")
    private String username;
    
    @Schema(description = "사용자 권한", example = "USER")
    private String role;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Schema(description = "계정 생성 일시", example = "2023-05-18T10:30:00.000")
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Schema(description = "계정 업데이트 일시", example = "2023-05-18T10:30:00.000")
    private LocalDateTime updatedAt;
    
    @Schema(description = "로그인 제공자", example = "local")
    private String provider;
    
    public static UserResponseDto of(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .provider(user.getProvider())
                .build();
    }
}
