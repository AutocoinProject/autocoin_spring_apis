package com.autocoin.news.api;

import com.autocoin.news.config.NewsApiConfig;
import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import com.autocoin.news.application.service.CryptoNewsService;
import com.autocoin.news.infrastructure.external.SerpApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "뉴스 테스트", description = "뉴스 테스트 API")
@RestController
@RequestMapping("/api/v1/news/test")
@RequiredArgsConstructor
@Slf4j
public class NewsTestController {

    private final CryptoNewsRepository cryptoNewsRepository;
    private final CryptoNewsService cryptoNewsService;
    private final SerpApiClient serpApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "테스트 뉴스 추가", description = "테스트 목적으로 샘플 뉴스 하나를 DB에 추가합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "뉴스 추가 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CryptoNewsDto.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CryptoNewsDto> addTestNews() {
        try {
            // 테스트 뉴스 생성
            CryptoNews testNews = CryptoNews.builder()
                    .title("영호쿠폰 - 테스트 뉴스")
                    .link("https://example.com/test-news-" + System.currentTimeMillis())
                    .source("테스트 소스")
                    .date(LocalDateTime.now().toString())
                    .thumbnail("https://example.com/thumbnail.jpg")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // DB에 저장
            CryptoNews savedNews = cryptoNewsRepository.save(testNews);
            log.info("테스트 뉴스가 DB에 저장되었습니다. ID: {}", savedNews.getId());
            
            // DTO 변환 후 반환
            CryptoNewsDto newsDto = CryptoNewsDto.fromEntity(savedNews);
            return ResponseEntity.ok(newsDto);
        } catch (Exception e) {
            log.error("테스트 뉴스 저장 중 오류: {}", e.getMessage());
            throw new RuntimeException("테스트 뉴스 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    

    @Operation(summary = "SerpAPI 뉴스 가져오기", description = "SerpAPI를 사용하여 실제 뉴스를 가져오고 DB에 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "뉴스 가져오기 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CryptoNewsDto.class))),
        @ApiResponse(responseCode = "500", description = "API 호출 실패 또는 서버 오류", content = @Content)
    })
    @GetMapping("/fetch-from-api")
    public ResponseEntity<List<CryptoNewsDto>> fetchNewsFromApi() {
        try {
            log.info("SerpAPI를 통한 뉴스 가져오기 시작");
            
            // SerpApiClient를 사용하여 API 호출
            Map<String, Object> apiResponse = serpApiClient.fetchCryptoNews();
            log.info("API 응답 받음: {}", apiResponse.size());
            
            // 데이터 파싱
            List<CryptoNewsDto> newsList = parseApiResponse(apiResponse);
            log.info("파싱된 뉴스 개수: {}", newsList.size());
            
            // DB에 저장
            for (CryptoNewsDto newsDto : newsList) {
                if (!cryptoNewsRepository.existsByLink(newsDto.getLink())) {
                    // 처음에는 DTO를 엔티티로 변환
                    CryptoNews entity = newsDto.toEntity();
                    // 저장 전 엔티티 확인
                    log.info("저장 전 엔티티 - ID: {}, 제목: {}", entity.getId(), entity.getTitle());
                    // 저장
                    CryptoNews savedNews = cryptoNewsRepository.save(entity);
                    // 저장 후 엔티티 확인
                    log.info("뉴스 저장 성공 - ID: {}, 제목: {}", savedNews.getId(), savedNews.getTitle());
                } else {
                    log.info("이미 존재하는 뉴스 링크: {}", newsDto.getLink());
                }
            }
            
            return ResponseEntity.ok(newsList);
        } catch (Exception e) {
            log.error("SerpAPI 호출 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("SerpAPI를 통한 뉴스 가져오기 실패: " + e.getMessage());
        }
    }
    
    /**
     * API 응답 Map을 파싱하여 뉴스 DTO 목록으로 변환
     */
    @SuppressWarnings("unchecked")
    private List<CryptoNewsDto> parseApiResponse(Map<String, Object> apiResponse) {
        List<CryptoNewsDto> newsList = new ArrayList<>();
        
        try {
            if (apiResponse != null && apiResponse.containsKey("news_results")) {
                List<Map<String, Object>> newsResults = (List<Map<String, Object>>) apiResponse.get("news_results");
                
                int count = 0;
                for (Map<String, Object> newsItem : newsResults) {
                    if (count >= 5) break;
                    
                    String title = (String) newsItem.get("title");
                    String link = (String) newsItem.get("link");
                    String source = (String) newsItem.get("source");
                    String date = (String) newsItem.get("date");
                    String thumbnail = (String) newsItem.get("thumbnail");
                    
                    log.info("파싱된 뉴스 - 제목: {}, 출처: {}, 날짜: {}", title, source, date);
                    
                    CryptoNewsDto newsDto = CryptoNewsDto.builder()
                            .title(title != null ? title : "")
                            .link(link != null ? link : "")
                            .source(source != null ? source : "")
                            .date(date != null ? date : "")
                            .thumbnail(thumbnail != null ? thumbnail : "")
                            .build();
                    
                    if (!newsDto.getTitle().isEmpty() && !newsDto.getLink().isEmpty()) {
                        newsList.add(newsDto);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("데이터 파싱 오류: {}", e.getMessage(), e);
            throw new RuntimeException("뉴스 데이터 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return newsList;
    }
    
    @Operation(summary = "저장된 전체 뉴스 조회", description = "데이터베이스에 저장된 모든 뉴스를 조회합니다 (개수 제한 없음).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CryptoNewsDto.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/all")
    public ResponseEntity<List<CryptoNewsDto>> getAllSavedNews() {
        try {
            List<CryptoNews> newsEntities = cryptoNewsRepository.findAll();
            log.info("DB에서 조회한 뉴스 개수: {}", newsEntities.size());
            
            // 각 엔티티의 ID 값 확인
            for (CryptoNews entity : newsEntities) {
                log.info("DB 엔티티 - ID: {}, 제목: {}", entity.getId(), entity.getTitle());
            }
            
            List<CryptoNewsDto> newsDtos = newsEntities.stream()
                    .map(entity -> {
                        CryptoNewsDto dto = CryptoNewsDto.fromEntity(entity);
                        log.info("DTO 변환 후 - ID: {}, 제목: {}", dto.getId(), dto.getTitle());
                        return dto;
                    })
                    .toList();
            
            // 최종 JSON 변환 전 확인
            try {
                String jsonString = objectMapper.writeValueAsString(newsDtos);
                log.info("JSON 변환 결과: {}", jsonString);
            } catch (Exception e) {
                log.error("JSON 변환 중 오류: {}", e.getMessage());
            }
            
            log.info("저장된 뉴스 조회 성공 - 총 {} 개", newsDtos.size());
            return ResponseEntity.ok(newsDtos);
        } catch (Exception e) {
            log.error("저장된 뉴스 조회 중 오류: {}", e.getMessage());
            throw new RuntimeException("저장된 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}