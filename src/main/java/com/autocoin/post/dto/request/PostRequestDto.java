package com.autocoin.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "게시글 작성 및 수정 요청 DTO")
public class PostRequestDto {

    @Schema(description = "게시글 제목", example = "안녕하세요, 처음 작성하는 게시글입니다.")
    private String title;
    
    @Schema(description = "게시글 내용", example = "게시글 내용입니다. 여러 줄의 텍스트를 입력할 수 있습니다.")
    private String content;
    
    @Schema(description = "게시글 작성자", example = "user123")
    private String writer;
    
    @Schema(description = "카테고리 ID", example = "2")
    private Long categoryId;
    
    @Schema(description = "업로드할 파일 (선택 사항)", nullable = true)
    private MultipartFile file;
}