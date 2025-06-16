package com.autocoin.news.dto.request;

import com.autocoin.news.domain.enums.NewsCategory;
import com.autocoin.news.domain.enums.NewsSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCreateRequest {
    
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    private String description;
    
    @NotBlank(message = "URL은 필수입니다")
    private String url;
    
    private String imageUrl;
    
    private String thumbnail;
    
    @NotBlank(message = "소스명은 필수입니다")
    private String sourceName;
    
    @NotNull(message = "발행일시는 필수입니다")
    private LocalDateTime publishedAt;
    
    private String content;
    
    @Builder.Default
    private NewsCategory category = NewsCategory.GENERAL;
    
    @Builder.Default
    private NewsSource source = NewsSource.MANUAL;
}
