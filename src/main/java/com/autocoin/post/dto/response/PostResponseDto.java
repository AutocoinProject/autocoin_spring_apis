package com.autocoin.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.autocoin.post.domain.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "게시글 응답 DTO")
public class PostResponseDto {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;
    
    @Schema(description = "게시글 제목", example = "안녕하세요, 처음 작성하는 게시글입니다.")
    private String title;
    
    @Schema(description = "게시글 내용", example = "게시글 내용입니다. 여러 줄의 텍스트를 입력할 수 있습니다.")
    private String content;
    
    @Schema(description = "게시글 작성자", example = "user123")
    private String writer;
    
    @Schema(description = "파일 URL", example = "https://example-bucket.s3.ap-northeast-2.amazonaws.com/posts/abc123.jpg", nullable = true)
    private String fileUrl;
    
    @Schema(description = "파일 이름", example = "image.jpg", nullable = true)
    private String fileName;
    
    @Schema(description = "카테고리 ID", example = "2", nullable = true)
    private Long categoryId;
    
    @Schema(description = "카테고리 이름", example = "자유게시판", nullable = true)
    private String categoryName;
    
    @Schema(description = "게시글 작성 일시", example = "2025-05-08T14:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "게시글 수정 일시", example = "2025-05-08T15:45:00")
    private LocalDateTime updatedAt;

    public static PostResponseDto of(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(post.getWriter())
                .fileUrl(post.getFileUrl())
                .fileName(post.getFileName())
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}