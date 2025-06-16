package com.autocoin.news.infrastructure.external;

import com.autocoin.news.config.NewsApiConfig;
import com.autocoin.news.domain.enums.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SerpAPI를 통한 뉴스 데이터 수집 전담 클라이언트
 * 외부 API 호출의 모든 책임을 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SerpApiClient {
    
    private final RestTemplate restTemplate;
    private final NewsApiConfig newsApiConfig;
    
    /**
     * 키워드별 뉴스 수집
     * @param keyword 검색할 키워드
     * @param maxCount 최대 결과 개수
     * @return API 응답 데이터
     */
    public Map<String, Object> fetchNewsByKeyword(String keyword, int maxCount) {
        try {
            // API 키 유효성 검증
            if (!newsApiConfig.isApiKeyValid()) {
                log.warn("SERP API 키가 유효하지 않습니다. 빈 결과를 반환합니다.");
                return new HashMap<>();
            }
            
            String url = newsApiConfig.getSerpApiUrlWithKeyword(keyword, maxCount);
            log.info("SerpAPI 호출 시작 - 키워드: '{}', 최대 개수: {}", keyword, maxCount);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            int newsCount = extractNewsCount(response);
            
            log.info("SerpAPI 응답 수신 완료 - 키워드: '{}', 수신된 뉴스: {}개", keyword, newsCount);
            return response != null ? response : new HashMap<>();
            
        } catch (RestClientException e) {
            log.error("SerpAPI 호출 중 네트워크 오류 발생 - 키워드: '{}', 오류: {}", keyword, e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            log.error("SerpAPI 호출 중 예상치 못한 오류 발생 - 키워드: '{}'", keyword, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 기본 암호화폐 뉴스 수집
     * @return API 응답 데이터
     */
    public Map<String, Object> fetchCryptoNews() {
        return fetchNewsByKeyword("crypto news", 5);
    }
    
    /**
     * 특정 카테고리별 뉴스 수집
     * @param category 뉴스 카테고리
     * @param maxCount 최대 결과 개수
     * @return API 응답 데이터
     */
    public Map<String, Object> fetchNewsByCategory(NewsCategory category, int maxCount) {
        String keyword = buildKeywordByCategory(category);
        return fetchNewsByKeyword(keyword, maxCount);
    }
    
    /**
     * API 키 유효성 확인
     * @return 유효성 여부
     */
    public boolean isApiKeyValid() {
        return newsApiConfig.isApiKeyValid();
    }
    
    /**
     * 카테고리별 검색 키워드 생성
     */
    private String buildKeywordByCategory(NewsCategory category) {
        switch (category) {
            case CRYPTO:
                return "cryptocurrency bitcoin";
            case BLOCKCHAIN:
                return "blockchain technology";
            case MARKET:
                return "crypto market";
            default:
                return "crypto news";
        }
    }
    
    /**
     * 응답에서 뉴스 개수 추출
     */
    private int extractNewsCount(Map<String, Object> response) {
        if (response != null && response.containsKey("news_results")) {
            List<?> newsResults = (List<?>) response.get("news_results");
            return newsResults != null ? newsResults.size() : 0;
        }
        return 0;
    }
}
