package com.autocoin.category.dto.response;

import com.autocoin.category.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카테고리 응답 DTO")
public class CategoryResponseDto {
    
    @Schema(description = "카테고리 ID")
    private Long id;
    
    @Schema(description = "카테고리 이름")
    private String name;
    
    @Schema(description = "카테고리 설명")
    private String description;
    
    @Schema(description = "상위 카테고리 ID")
    private Long parentId;
    
    @Schema(description = "상위 카테고리 이름")
    private String parentName;
    
    @Schema(description = "하위 카테고리 목록")
    private List<CategoryResponseDto> children;
    
    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;
    
    // Entity -> DTO 변환 메소드 (자식 카테고리 포함)
    public static CategoryResponseDto of(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .children(category.getChildren().stream()
                        .map(CategoryResponseDto::of)
                        .collect(Collectors.toList()))
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
    
    // Entity -> DTO 변환 메소드 (자식 카테고리 제외 - 순환 참조 방지)
    public static CategoryResponseDto ofWithoutChildren(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .children(null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
    
    // DTO -> Entity 변환 메서드 (PostService에서 사용)
    public Category toEntity() {
        return Category.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .build();
    }
}
