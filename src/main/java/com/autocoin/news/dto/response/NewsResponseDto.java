package com.autocoin.news.dto.response;

import com.autocoin.news.domain.entity.News;
import com.autocoin.news.domain.enums.NewsCategory;
import com.autocoin.news.domain.enums.NewsSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponseDto {
    private Long id;
    private String title;
    private String description;
    private String url;
    private String imageUrl;
    private String thumbnail;
    private String sourceName;
    private NewsSource source;
    private LocalDateTime publishedAt;
    private String content;
    private NewsCategory category;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static NewsResponseDto from(News news) {
        return NewsResponseDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .description(news.getDescription())
                .url(news.getUrl())
                .imageUrl(news.getImageUrl())
                .thumbnail(news.getThumbnail())
                .sourceName(news.getSourceName())
                .source(news.getSource())
                .publishedAt(news.getPublishedAt())
                .content(news.getContent())
                .category(news.getCategory())
                .viewCount(news.getViewCount())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
    
    public static NewsResponseDto fromWithoutContent(News news) {
        return NewsResponseDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .description(news.getDescription())
                .url(news.getUrl())
                .imageUrl(news.getImageUrl())
                .thumbnail(news.getThumbnail())
                .sourceName(news.getSourceName())
                .source(news.getSource())
                .publishedAt(news.getPublishedAt())
                .category(news.getCategory())
                .viewCount(news.getViewCount())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}