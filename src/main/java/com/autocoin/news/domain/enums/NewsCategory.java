package com.autocoin.news.domain.enums;

/**
 * 뉴스 카테고리 열거형
 */
public enum NewsCategory {
    GENERAL("일반"),
    CRYPTO("암호화폐"),
    BLOCKCHAIN("블록체인"),
    TECH("기술"),
    FINANCE("금융"),
    MARKET("시장");

    private final String description;

    NewsCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
