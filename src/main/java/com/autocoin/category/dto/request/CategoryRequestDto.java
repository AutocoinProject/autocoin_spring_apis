package com.autocoin.category.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카테고리 생성 및 수정 요청 DTO")
public class CategoryRequestDto {
    
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    @Schema(description = "카테고리 이름", example = "자유게시판")
    private String name;
    
    @Schema(description = "카테고리 설명", example = "자유롭게 대화를 나누는 공간입니다.")
    private String description;
    
    @Schema(description = "상위 카테고리 ID (없으면 루트 카테고리)", example = "1", nullable = true)
    private Long parentId;
}
