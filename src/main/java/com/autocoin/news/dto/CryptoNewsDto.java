package com.autocoin.news.dto;

import com.autocoin.news.domain.entity.CryptoNews;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoNewsDto {
    private Long id;
    private String title;
    private String link;
    private String source;
    private String date;
    private String thumbnail;
    
    public static CryptoNewsDto fromEntity(CryptoNews entity) {
        return CryptoNewsDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .link(entity.getLink())
                .source(entity.getSource())
                .date(entity.getDate())
                .thumbnail(entity.getThumbnail())
                .build();
    }
    
    public CryptoNews toEntity() {
        return CryptoNews.builder()
                .id(this.id) // ID 필드 추가
                .title(this.title)
                .link(this.link)
                .source(this.source)
                .date(this.date)
                .thumbnail(this.thumbnail)
                .build();
    }
}
