package com.autocoin.news.api.controller;

import com.autocoin.news.application.service.NewsService;
import com.autocoin.news.dto.response.NewsPageResponseDto;
import com.autocoin.news.dto.response.NewsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "뉴스 관리 API")
public class NewsController {
    
    private final NewsService newsService;
    
    @GetMapping
    @Operation(summary = "뉴스 목록 조회", description = "페이지네이션과 카테고리 필터링을 지원하는 뉴스 목록을 조회합니다.")
    public ResponseEntity<NewsPageResponseDto> getNews(
            @Parameter(description = "카테고리 (CRYPTOCURRENCY, BLOCKCHAIN, FINANCE, TECHNOLOGY, MARKET, BITCOIN, ETHEREUM)")
            @RequestParam(required = false) String category,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size) {
        NewsPageResponseDto news = newsService.getLatestNews(category, page, size);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "뉴스 상세 조회", description = "뉴스 ID로 상세 정보를 조회합니다. 조회수가 증가합니다.")
    public ResponseEntity<NewsResponseDto> getNewsById(
            @Parameter(description = "뉴스 ID")
            @PathVariable Long id) {
        NewsResponseDto news = newsService.getNewsById(id);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/popular")
    @Operation(summary = "인기 뉴스 조회", description = "조회수 기준 상위 5개 뉴스를 조회합니다.")
    public ResponseEntity<List<NewsResponseDto>> getPopularNews() {
        List<NewsResponseDto> popularNews = newsService.getPopularNews();
        return ResponseEntity.ok(popularNews);
    }
    
    @PostMapping("/collect")
    @Operation(summary = "뉴스 수집 실행", description = "즉시 암호화폐 뉴스를 수집합니다.")
    public ResponseEntity<Map<String, String>> collectNews() {
        newsService.collectCryptocurrencyNews();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "뉴스 수집이 시작되었습니다."
        ));
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "뉴스 통계 조회", description = "뉴스 통계 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getNewsStatistics() {
        Map<String, Object> statistics = newsService.getNewsStatistics();
        return ResponseEntity.ok(statistics);
    }
}