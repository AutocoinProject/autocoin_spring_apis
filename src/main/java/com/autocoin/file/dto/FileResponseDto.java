package com.autocoin.file.dto;

import com.autocoin.file.domain.File;
import com.autocoin.user.dto.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto {
    private Long id;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private UserResponseDto user;
    private LocalDateTime createdAt;
    
    public static FileResponseDto of(File file) {
        return FileResponseDto.builder()
                .id(file.getId())
                .originalFileName(file.getOriginalFileName())
                .fileUrl(file.getFileUrl())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .user(UserResponseDto.of(file.getUser()))
                .createdAt(file.getCreatedAt())
                .build();
    }
}
