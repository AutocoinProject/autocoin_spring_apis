package com.autocoin.news.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 뉴스 API 설정을 관리하는 Configuration 클래스
 * SERP API를 사용한 암호화폐 뉴스 수집 설정을 담당합니다.
 */
@Configuration
public class NewsApiConfig {

    @Value("${serp.api.key:demo}")
    private String serpApiKey;

    /**
     * SERP API 키를 반환합니다.
     * @return SERP API 키
     */
    public String getSerpApiKey() {
        return serpApiKey;
    }

    /**
     * 암호화폐 뉴스 검색을 위한 SERP API URL을 생성합니다.
     * @return 완전한 API URL
     */
    public String getSerpApiUrl() {
        return "https://serpapi.com/search.json?q=crypto+news&tbm=nws&api_key=" + serpApiKey;
    }

    /**
     * 특정 키워드로 뉴스를 검색하기 위한 SERP API URL을 생성합니다.
     * @param keyword 검색할 키워드
     * @param maxCount 최대 결과 개수
     * @return 키워드 기반 API URL
     */
    public String getSerpApiUrlWithKeyword(String keyword, int maxCount) {
        return String.format(
            "https://serpapi.com/search.json?q=%s&tbm=nws&num=%d&api_key=%s",
            keyword.replace(" ", "+"), maxCount, serpApiKey
        );
    }

    /**
     * API 키가 유효한지 확인합니다.
     * @return API 키 유효성 여부
     */
    public boolean isApiKeyValid() {
        return serpApiKey != null && 
               !serpApiKey.trim().isEmpty() && 
               !"demo".equals(serpApiKey) &&
               !"test-key".equals(serpApiKey) &&
               !"your-default-serp-api-key".equals(serpApiKey);
    }
}
