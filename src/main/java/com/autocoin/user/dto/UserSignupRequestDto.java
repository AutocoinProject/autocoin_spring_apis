package com.autocoin.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class UserSignupRequestDto {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "사용자 이메일", example = "test@autocoin.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", 
             message = "비밀번호는 영문자, 숫자, 특수문자를 포함해야 합니다.")
    @Schema(description = "사용자 비밀번호 (영문자, 숫자, 특수문자 포함, 8자 이상)", example = "Test1234!")
    private String password;

    @NotBlank(message = "사용자 이름은 필수 입력값입니다.")
    @Size(min = 2, max = 10, message = "사용자 이름은 2~10자 사이여야 합니다.")
    @Schema(description = "사용자 이름 (2~10자)", example = "테스트사용자")
    private String username;
}
