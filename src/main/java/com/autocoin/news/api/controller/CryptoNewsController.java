package com.autocoin.news.api;

import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.application.service.CryptoNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "뉴스 API", description = "암호화폐 뉴스 관련 API")
@RestController
@RequestMapping("/api/v1/crypto-news")
@RequiredArgsConstructor
public class CryptoNewsController {

    private final CryptoNewsService cryptoNewsService;

    @Operation(summary = "암호화폐 뉴스 조회", description = "SerpAPI를 통해 최신 암호화폐 뉴스를 가져옵니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류 - API 호출 실패")
    })
    @GetMapping
    public ResponseEntity<List<CryptoNewsDto>> getNews() {
        try {
            List<CryptoNewsDto> newsList = cryptoNewsService.getCryptoNews();
            return ResponseEntity.ok(newsList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @Operation(summary = "저장된 암호화폐 뉴스 조회", description = "데이터베이스에 저장된 암호화폐 뉴스를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/saved")
    public ResponseEntity<List<CryptoNewsDto>> getSavedNews() {
        try {
            List<CryptoNewsDto> newsList = cryptoNewsService.getSavedNews();
            return ResponseEntity.ok(newsList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}
