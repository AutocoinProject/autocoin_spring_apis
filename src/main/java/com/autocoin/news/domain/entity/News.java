package com.autocoin.news.domain.entity;

import com.autocoin.global.domain.BaseEntity;
import com.autocoin.news.domain.enums.NewsCategory;
import com.autocoin.news.domain.enums.NewsSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, unique = true)
    private String url;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private String sourceName;
    
    @Column(nullable = false)
    private LocalDateTime publishedAt;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NewsCategory category = NewsCategory.GENERAL;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NewsSource source = NewsSource.MANUAL;
    
    @Builder.Default
    private Integer viewCount = 0;
    
    // 썸네일 이미지 (CryptoNews와 호환)
    private String thumbnail;
    
    public void updateContent(String content) {
        this.content = content;
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void updateCategory(NewsCategory category) {
        this.category = category;
    }
    
    public void updateSource(NewsSource source) {
        this.source = source;
    }
}