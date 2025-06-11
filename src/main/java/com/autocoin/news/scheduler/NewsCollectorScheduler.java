package com.autocoin.news.scheduler;

import com.autocoin.news.config.NewsApiConfig;
import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import com.autocoin.news.application.service.CryptoNewsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 주기적으로 암호화폐 뉴스를 수집하여 데이터베이스에 저장하는 스케줄러 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCollectorScheduler {

    private final RestTemplate restTemplate;
    private final NewsApiConfig newsApiConfig;
    private final CryptoNewsRepository cryptoNewsRepository;
    private final CryptoNewsService cryptoNewsService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${news.scheduler.collect:0 0 * * * *}")
    private String newsCollectCron;
    
    @Value("${news.scheduler.cleanup:0 0 15 * * *}")
    private String newsCleanupCron;
    
    @Value("${news.scheduler.max-news-count:50}")
    private int maxNewsCount;
    
    /**
     * 1시간마다 최신 암호화폐 뉴스 1개를 가져와 데이터베이스에 저장합니다.
     * cron 표현식: 초 분 시 일 월 요일
     * "0 0 * * * *" - 매 시간 정각마다 실행
     */
    @Scheduled(cron = "${news.scheduler.collect:0 0 * * * *}")
    @Transactional
    public void collectLatestNews() {
        log.info("뉴스 수집 스케줄러 실행: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        try {
            String apiUrl = newsApiConfig.getSerpApiUrl();
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            // 최신 뉴스 1개 파싱 및 저장
            Optional<CryptoNewsDto> latestNews = parseLatestNews(response);
            
            if (latestNews.isPresent()) {
                saveNewsIfNotExists(latestNews.get());
                log.info("최신 뉴스 저장 완료: {}", latestNews.get().getTitle());
            } else {
                log.warn("파싱할 뉴스가 없습니다.");
            }
            
        } catch (RestClientException e) {
            log.error("뉴스 수집 중 API 오류 발생: {}", e.getMessage());
        } catch (Exception e) {
            log.error("뉴스 수집 중 예외 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 매일 전체 데이터베이스 정리를 위해 오래된 뉴스를 삭제합니다.
     * 매일 오후 3시에 실행
     */
    @Scheduled(cron = "${news.scheduler.cleanup:0 0 15 * * *}")
    @Transactional
    public void cleanupDatabase() {
        log.info("데이터베이스 정리 스케줄러 실행: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // 설정된 최대 뉴스 개수만 유지
        int deletedCount = cryptoNewsService.cleanupOldNews(maxNewsCount);
        
        log.info("데이터베이스 정리 완료. 삭제된 뉴스 개수: {}", deletedCount);
    }
    
    /**
     * SerpAPI 응답에서 최신 뉴스 1개만 파싱합니다.
     */
    private Optional<CryptoNewsDto> parseLatestNews(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode newsResults = rootNode.path("news_results");
            
            if (newsResults.isArray() && newsResults.size() > 0) {
                // 첫 번째 뉴스 항목만 가져옴
                JsonNode newsItem = newsResults.get(0);
                
                CryptoNewsDto newsDto = CryptoNewsDto.builder()
                        .title(newsItem.path("title").asText(""))
                        .link(newsItem.path("link").asText(""))
                        .source(newsItem.path("source").asText(""))
                        .date(newsItem.path("date").asText(""))
                        .thumbnail(newsItem.path("thumbnail").asText(""))
                        .build();
                
                // 필수 필드 검증
                if (!newsDto.getTitle().isEmpty() && !newsDto.getLink().isEmpty()) {
                    return Optional.of(newsDto);
                }
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("JSON 파싱 오류: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 뉴스가 데이터베이스에 존재하지 않는 경우에만 저장합니다.
     */
    private void saveNewsIfNotExists(CryptoNewsDto newsDto) {
        // 링크로 중복 확인
        if (!cryptoNewsRepository.existsByLink(newsDto.getLink())) {
            CryptoNews newsEntity = newsDto.toEntity();
            cryptoNewsRepository.save(newsEntity);
            log.info("새로운 뉴스 항목 저장: {}", newsDto.getTitle());
        } else {
            log.info("중복 뉴스 무시: {}", newsDto.getTitle());
        }
    }
    
    /**
     * 테스트용 메서드: 1분마다 실행되는 스케줄링 (개발 중에만 사용)
     * 주석을 해제하여 테스트할 수 있습니다.
     */
    //@Scheduled(fixedRate = 60000) // 1분마다 실행 (60000 밀리초 = 1분)
    public void testCollectNews() {
        log.info("테스트 뉴스 수집 실행: {}", LocalDateTime.now());
        collectLatestNews();
    }
}
