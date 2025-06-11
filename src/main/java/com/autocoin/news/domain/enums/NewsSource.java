package com.autocoin.news.domain.enums;

/**
 * 뉴스 소스 열거형
 */
public enum NewsSource {
    SERP_API("SerpAPI"),
    MANUAL("수동 입력"),
    RSS("RSS 피드"),
    EXTERNAL_API("외부 API"),
    CRAWLER("웹 크롤링");

    private final String description;

    NewsSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
