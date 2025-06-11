package com.autocoin.news.dto.request;

import com.autocoin.news.domain.enums.NewsCategory;
import com.autocoin.news.domain.enums.NewsSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSearchRequest {
    
    private String keyword;
    private NewsCategory category;
    private NewsSource source;
    private String sourceName;
    
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 20;
    
    @Builder.Default
    private String sortBy = "publishedAt";
    
    @Builder.Default
    private String sortDirection = "desc";
}
