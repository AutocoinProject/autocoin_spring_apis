package com.autocoin.global.dev;

import lombok.Builder;
import lombok.Getter;

/**
 * 개발용 사용자 정보 DTO
 */
@Getter
@Builder
public class DevUserInfo {
    private Long id;
    private String email;
    private String username;
    private String role;
}
