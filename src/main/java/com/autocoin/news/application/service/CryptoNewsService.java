package com.autocoin.news.application.service;

import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.infrastructure.external.SerpApiClient;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoNewsService {

    private final SerpApiClient serpApiClient;
    private final CryptoNewsRepository cryptoNewsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 암호화폐 뉴스 데이터를 수집하고 처리합니다.
     * 외부 API 호출은 SerpApiClient에 위임하고, 비즈니스 로직만 처리합니다.
     * @return 암호화폐 뉴스 목록 (최대 5개)
     */
    @Transactional
    public List<CryptoNewsDto> getCryptoNews() {
        try {
            // 외부 API 호출은 SerpApiClient에 위임
            Map<String, Object> apiResponse = serpApiClient.fetchCryptoNews();
            
            // 비즈니스 로직: 응답 데이터 파싱 및 변환
            List<CryptoNewsDto> newsList = parseApiResponse(apiResponse);
            
            // 비즈니스 로직: DB에 저장 (중복 방지)
            saveToDatabase(newsList);
            
            return newsList;
        } catch (Exception e) {
            log.error("암호화폐 뉴스 데이터 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("뉴스 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * API 응답 Map을 파싱하여 뉴스 DTO 목록으로 변환
     * @param apiResponse SerpApiClient로부터 받은 응답 데이터
     * @return 파싱된 뉴스 목록
     */
    @SuppressWarnings("unchecked")
    private List<CryptoNewsDto> parseApiResponse(Map<String, Object> apiResponse) {
        List<CryptoNewsDto> newsList = new ArrayList<>();
        
        try {
            if (apiResponse != null && apiResponse.containsKey("news_results")) {
                List<Map<String, Object>> newsResults = (List<Map<String, Object>>) apiResponse.get("news_results");
                
                int count = 0;
                for (Map<String, Object> newsItem : newsResults) {
                    if (count >= 5) break; // 최대 5개만 가져오기
                    
                    CryptoNewsDto newsDto = buildCryptoNewsDto(newsItem);
                    
                    // 필수 필드 검증
                    if (isValidNewsDto(newsDto)) {
                        newsList.add(newsDto);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("API 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("뉴스 데이터 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return newsList;
    }
    
    /**
     * Map 데이터를 CryptoNewsDto로 변환
     */
    private CryptoNewsDto buildCryptoNewsDto(Map<String, Object> newsItem) {
        return CryptoNewsDto.builder()
                .title((String) newsItem.get("title"))
                .link((String) newsItem.get("link"))
                .source((String) newsItem.get("source"))
                .date((String) newsItem.get("date"))
                .thumbnail((String) newsItem.get("thumbnail"))
                .build();
    }
    
    /**
     * 뉴스 DTO 유효성 검증
     */
    private boolean isValidNewsDto(CryptoNewsDto newsDto) {
        return newsDto.getTitle() != null && !newsDto.getTitle().trim().isEmpty() &&
               newsDto.getLink() != null && !newsDto.getLink().trim().isEmpty();
    }

    /**
     * 뉴스 데이터를 데이터베이스에 저장합니다 (중복 방지).
     * @param newsList 저장할 뉴스 목록
     */
    private void saveToDatabase(List<CryptoNewsDto> newsList) {
        for (CryptoNewsDto newsDto : newsList) {
            // 중복 링크 확인
            if (!cryptoNewsRepository.existsByLink(newsDto.getLink())) {
                CryptoNews newsEntity = newsDto.toEntity();
                cryptoNewsRepository.save(newsEntity);
            }
        }
    }

    /**
     * 데이터베이스에서 저장된 뉴스를 검색합니다.
     * @return 저장된 뉴스 목록 (최대 5개)
     */
    @Transactional(readOnly = true)
    public List<CryptoNewsDto> getSavedNews() {
        List<CryptoNews> newsEntities = cryptoNewsRepository.findAll();
        List<CryptoNewsDto> newsDtos = new ArrayList<>();
        
        int count = 0;
        for (CryptoNews entity : newsEntities) {
            if (count >= 5) break; // 최대 5개만 반환
            newsDtos.add(CryptoNewsDto.fromEntity(entity));
            count++;
        }
        
        return newsDtos;
    }
    
    /**
     * 오래된 뉴스를 삭제합니다. 데이터베이스 크기 관리를 위해 사용됩니다.
     * @param maxNewsCount 유지할 최대 뉴스 개수
     * @return 삭제된 뉴스 개수
     */
    @Transactional
    public int cleanupOldNews(int maxNewsCount) {
        // 전체 뉴스 개수 확인
        long totalNewsCount = cryptoNewsRepository.count();
        
        // 최대 개수보다 적으면 삭제 없음
        if (totalNewsCount <= maxNewsCount) {
            return 0;
        }
        
        // 삭제할 개수 계산
        long countToDelete = totalNewsCount - maxNewsCount;
        
        // 최대 100개 제한 (대량 삭제 방지)
        countToDelete = Math.min(countToDelete, 100);
        
        // 가장 오래된 뉴스 조회
        Pageable pageable = PageRequest.of(0, (int) countToDelete);
        List<CryptoNews> oldestNews = cryptoNewsRepository.findOldestNews(pageable);
        
        // 삭제 진행
        if (!oldestNews.isEmpty()) {
            List<Long> idsToDelete = oldestNews.stream()
                    .map(CryptoNews::getId)
                    .collect(Collectors.toList());
            
            cryptoNewsRepository.deleteAllByIdInBatch(idsToDelete);
            
            log.info("{}개의 오래된 뉴스 삭제 완료", idsToDelete.size());
            return idsToDelete.size();
        }
        
        return 0;
    }
}
