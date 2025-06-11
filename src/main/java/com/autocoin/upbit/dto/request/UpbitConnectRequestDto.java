package com.autocoin.upbit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "업비트 계정 연결 요청")
public class UpbitConnectRequestDto {
    
    @NotBlank(message = "액세스 키는 필수입니다.")
    @Schema(description = "업비트 액세스 키", example = "AQhxPyb7kmBRmFrrzbWwLvXesdxD6Boq14KDx1Oc")
    private String accessKey;
    
    @NotBlank(message = "시크릿 키는 필수입니다.")
    @Schema(description = "업비트 시크릿 키", example = "KvzVi4heCSAVZnWNlxFYJNLLP1ixWgpskpYM7CRp")
    private String secretKey;
    
    @Schema(description = "계정 별명", example = "My Upbit Account")
    private String nickname;
}