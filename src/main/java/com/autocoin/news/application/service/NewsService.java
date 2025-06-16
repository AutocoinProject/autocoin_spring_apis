package com.autocoin.news.application.service;

import com.autocoin.global.exception.core.CustomException;
import com.autocoin.global.exception.core.ErrorCode;
import com.autocoin.news.domain.enums.NewsCategory;
import com.autocoin.news.domain.enums.NewsSource;
import com.autocoin.news.domain.NewsRepository;
import com.autocoin.news.domain.entity.News;
import com.autocoin.news.dto.response.NewsPageResponseDto;
import com.autocoin.news.dto.response.NewsResponseDto;
import com.autocoin.news.infrastructure.external.SerpApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NewsService {
    private final NewsRepository newsRepository;
    private final SerpApiClient serpApiClient;
    
    @Scheduled(cron = "0 0 7 * * ?") // 매일 오전 7시에 실행
    @ConditionalOnProperty(name = "news.scheduler.enabled", havingValue = "true", matchIfMissing = true)
    @Transactional
    public void collectCryptocurrencyNews() {
        // API 키가 유효하지 않은 경우 뉴스 수집 스킵
        if (!serpApiClient.isApiKeyValid()) {
            log.info("SERP API 키가 설정되지 않아 뉴스 수집을 스킵합니다.");
            return;
        }
        
        log.info("암호화폐 뉴스 수집 시작 - 매일 오전 7시");
        
        try {
            // 각 카테고리별로 5개씩 뉴스 수집
            collectNewsByKeyword("cryptocurrency bitcoin", NewsCategory.CRYPTO, 5);
            collectNewsByKeyword("ethereum blockchain", NewsCategory.CRYPTO, 5);
            collectNewsByKeyword("crypto market", NewsCategory.MARKET, 5);
            collectNewsByKeyword("blockchain technology", NewsCategory.BLOCKCHAIN, 5);
            
            log.info("암호화폐 뉴스 수집 완료 - 총 20개 뉴스 수집 시도");
        } catch (Exception e) {
            log.error("뉴스 수집 중 오류 발생", e);
        }
    }
    
    /**
     * 키워드별 뉴스 수집 (비즈니스 로직)
     * 외부 API 호출은 SerpApiClient에 위임하고, 순수 비즈니스 로직만 처리
     */
    @SuppressWarnings("unchecked")
    private void collectNewsByKeyword(String keyword, NewsCategory category, int maxCount) {
        try {
            // 외부 API 호출은 SerpApiClient에 위임
            Map<String, Object> response = serpApiClient.fetchNewsByKeyword(keyword, maxCount);
            
            // 비즈니스 로직: 응답 데이터 처리 및 저장
            if (response != null && response.containsKey("news_results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> newsResults = (List<Map<String, Object>>) response.get("news_results");
                
                int newNewsCount = processAndSaveNews(newsResults, category, maxCount);
                log.info("키워드 '{}': 새로운 뉴스 {}개 수집 완료 (최대 {}개 제한)", keyword, newNewsCount, maxCount);
            } else {
                log.warn("키워드 '{}': 유효한 뉴스 데이터를 받지 못했습니다.", keyword);
            }
        } catch (Exception e) {
            log.error("키워드 '{}' 뉴스 수집 중 비즈니스 로직 오류 발생: {}", keyword, e.getMessage());
        }
    }
    
    /**
     * 뉴스 데이터 처리 및 저장
     */
    private int processAndSaveNews(List<Map<String, Object>> newsResults, NewsCategory category, int maxCount) {
        int newNewsCount = 0;
        int processedCount = 0;
        
        for (Map<String, Object> newsItem : newsResults) {
            if (processedCount >= maxCount) {
                break; // 최대 개수 제한
            }
            
            String newsUrl = (String) newsItem.get("link");
            
            if (newsUrl != null && !newsRepository.existsByUrl(newsUrl)) {
                News news = buildNewsEntity(newsItem, category);
                newsRepository.save(news);
                newNewsCount++;
                log.debug("새로운 뉴스 저장: {}", news.getTitle());
            }
            processedCount++;
        }
        
        return newNewsCount;
    }
    
    /**
     * 뉴스 엔티티 생성
     */
    private News buildNewsEntity(Map<String, Object> newsItem, NewsCategory category) {
        return News.builder()
                .title(cleanText((String) newsItem.get("title")))
                .description(cleanText((String) newsItem.get("snippet")))
                .url((String) newsItem.get("link"))
                .imageUrl((String) newsItem.get("thumbnail"))
                .sourceName(cleanText((String) newsItem.get("source")))
                .publishedAt(parseDate((String) newsItem.get("date")))
                .category(category)
                .source(NewsSource.SERP_API)
                .viewCount(0)
                .build();
    }
    
    @Transactional(readOnly = true)
    public NewsPageResponseDto getLatestNews(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<News> newsPage;
        
        if (category != null && !category.trim().isEmpty()) {
            try {
                NewsCategory newsCategory = NewsCategory.valueOf(category.toUpperCase());
                newsPage = newsRepository.findByCategoryOrderByPublishedAtDesc(newsCategory, pageable);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_CATEGORY);
            }
        } else {
            newsPage = newsRepository.findByOrderByPublishedAtDesc(pageable);
        }
        
        List<NewsResponseDto> newsResponseDtos = newsPage.getContent().stream()
                .map(NewsResponseDto::fromWithoutContent)
                .collect(Collectors.toList());
        
        return NewsPageResponseDto.builder()
                .content(newsResponseDtos)
                .pageNumber(newsPage.getNumber())
                .pageSize(newsPage.getSize())
                .totalElements(newsPage.getTotalElements())
                .totalPages(newsPage.getTotalPages())
                .first(newsPage.isFirst())
                .last(newsPage.isLast())
                .hasNext(newsPage.hasNext())
                .hasPrevious(newsPage.hasPrevious())
                .build();
    }
    
    @Transactional
    public NewsResponseDto getNewsById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND));
        
        // 조회수 증가
        news.incrementViewCount();
        newsRepository.save(news);
        
        return NewsResponseDto.from(news);
    }
    
    @Transactional(readOnly = true)
    public List<NewsResponseDto> getPopularNews() {
        List<News> popularNews = newsRepository.findTop5ByOrderByViewCountDesc();
        return popularNews.stream()
                .map(NewsResponseDto::fromWithoutContent)
                .collect(Collectors.toList());
    }
    
    @Scheduled(cron = "0 0 15 * * ?") // 매일 오후 3시
    @ConditionalOnProperty(name = "news.scheduler.enabled", havingValue = "true", matchIfMissing = true)
    @Transactional
    public void cleanupOldNews() {
        log.info("오래된 뉴스 정리 시작");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<News> oldNews = newsRepository.findByPublishedAtBefore(thirtyDaysAgo);
            
            if (!oldNews.isEmpty()) {
                newsRepository.deleteByPublishedAtBefore(thirtyDaysAgo);
                log.info("{}개의 오래된 뉴스를 삭제했습니다.", oldNews.size());
            } else {
                log.info("삭제할 오래된 뉴스가 없습니다.");
            }
        } catch (Exception e) {
            log.error("오래된 뉴스 정리 중 오류 발생", e);
        }
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getNewsStatistics() {
        long totalNews = newsRepository.findAll().size();
        long bitcoinNews = newsRepository.countByCategory(NewsCategory.CRYPTO);
        long ethereumNews = newsRepository.countByCategory(NewsCategory.CRYPTO);
        long blockchainNews = newsRepository.countByCategory(NewsCategory.BLOCKCHAIN);
        long marketNews = newsRepository.countByCategory(NewsCategory.MARKET);
        
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayNews = newsRepository.findAll().stream()
                .filter(news -> news.getPublishedAt().isAfter(today))
                .count();
        
        return Map.of(
                "totalNews", totalNews,
                "todayNews", todayNews,
                "categoryStats", Map.of(
                        "CRYPTO", bitcoinNews + ethereumNews,
                        "BLOCKCHAIN", blockchainNews,
                        "MARKET", marketNews
                )
        );
    }
    
    private LocalDateTime parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // 다양한 날짜 형식 시도
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("MMM dd, yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(dateString, formatter);
                } catch (DateTimeParseException e) {
                    // 다음 포맷터 시도
                }
            }
            
            // 모든 파싱 실패시 현재 시간 반환
            log.warn("날짜 파싱 실패: {}. 현재 시간으로 대체합니다.", dateString);
            return LocalDateTime.now();
            
        } catch (Exception e) {
            log.warn("날짜 파싱 중 오류 발생: {}. 현재 시간으로 대체합니다.", dateString);
            return LocalDateTime.now();
        }
    }
    
    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        
        // HTML 태그 제거 및 텍스트 정리
        return text.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&quot;", "\"")
                   .trim();
    }
}