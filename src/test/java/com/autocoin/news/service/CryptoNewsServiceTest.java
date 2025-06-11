package com.autocoin.news.service;

import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.infrastructure.external.SerpApiClient;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import com.autocoin.news.application.service.CryptoNewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CryptoNewsServiceTest {

    @Mock
    private SerpApiClient serpApiClient;

    @Mock
    private CryptoNewsRepository cryptoNewsRepository;

    @InjectMocks
    private CryptoNewsService cryptoNewsService;

    @Captor
    private ArgumentCaptor<CryptoNews> cryptoNewsCaptor;

    @Test
    @DisplayName("SerpAPI에서 뉴스 가져오기 성공 테스트")
    void getCryptoNewsSuccess() {
        // given
        Map<String, Object> mockApiResponse = createMockApiResponseMap();
        
        given(serpApiClient.fetchCryptoNews()).willReturn(mockApiResponse);
        given(cryptoNewsRepository.existsByLink(anyString())).willReturn(false);
        given(cryptoNewsRepository.save(any(CryptoNews.class))).willAnswer(invocation -> {
            CryptoNews news = invocation.getArgument(0);
            return CryptoNews.builder()
                    .id(1L)
                    .title(news.getTitle())
                    .link(news.getLink())
                    .source(news.getSource())
                    .date(news.getDate())
                    .thumbnail(news.getThumbnail())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        // when
        List<CryptoNewsDto> result = cryptoNewsService.getCryptoNews();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("뉴스 제목 1");
        assertThat(result.get(1).getTitle()).isEqualTo("뉴스 제목 2");
        
        verify(serpApiClient, times(1)).fetchCryptoNews();
        verify(cryptoNewsRepository, times(2)).existsByLink(anyString());
        verify(cryptoNewsRepository, times(2)).save(any(CryptoNews.class));
    }

    @Test
    @DisplayName("API 호출 실패 시 예외 발생 테스트")
    void getCryptoNewsApiFailure() {
        // given
        given(serpApiClient.fetchCryptoNews())
                .willThrow(new RuntimeException("API 호출 실패"));

        // when & then
        assertThatThrownBy(() -> cryptoNewsService.getCryptoNews())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("뉴스 데이터 처리 중 오류가 발생했습니다");
        
        verify(serpApiClient, times(1)).fetchCryptoNews();
        verifyNoInteractions(cryptoNewsRepository);
    }

    @Test
    @DisplayName("이미 존재하는 뉴스는 저장하지 않는 테스트")
    void getCryptoNewsSkipExistingLinks() {
        // given
        Map<String, Object> mockApiResponse = createMockApiResponseMap();
        
        given(serpApiClient.fetchCryptoNews()).willReturn(mockApiResponse);
        // 첫 번째 뉴스는 이미 존재함, 두 번째 뉴스는 존재하지 않음
        given(cryptoNewsRepository.existsByLink("https://example.com/news-1")).willReturn(true);
        given(cryptoNewsRepository.existsByLink("https://example.com/news-2")).willReturn(false);
        given(cryptoNewsRepository.save(any(CryptoNews.class))).willAnswer(invocation -> {
            CryptoNews news = invocation.getArgument(0);
            return CryptoNews.builder()
                    .id(1L)
                    .title(news.getTitle())
                    .link(news.getLink())
                    .source(news.getSource())
                    .date(news.getDate())
                    .thumbnail(news.getThumbnail())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        // when
        List<CryptoNewsDto> result = cryptoNewsService.getCryptoNews();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2); // API에서는 2개 가져오지만
        
        verify(cryptoNewsRepository, times(1)).existsByLink("https://example.com/news-1");
        verify(cryptoNewsRepository, times(1)).existsByLink("https://example.com/news-2");
        verify(cryptoNewsRepository, times(1)).save(any(CryptoNews.class)); // 저장은 1개만 됨
    }

    @Test
    @DisplayName("저장된 뉴스 조회 테스트")
    void getSavedNewsSuccess() {
        // given
        List<CryptoNews> savedNews = Arrays.asList(
                createTestCryptoNews(1L, "저장된 뉴스 1"),
                createTestCryptoNews(2L, "저장된 뉴스 2"),
                createTestCryptoNews(3L, "저장된 뉴스 3"),
                createTestCryptoNews(4L, "저장된 뉴스 4"),
                createTestCryptoNews(5L, "저장된 뉴스 5"),
                createTestCryptoNews(6L, "저장된 뉴스 6") // 6개 지만 5개만 반환해야 함
        );
        
        given(cryptoNewsRepository.findAll()).willReturn(savedNews);

        // when
        List<CryptoNewsDto> result = cryptoNewsService.getSavedNews();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5); // 최대 5개만 반환
        assertThat(result.get(0).getTitle()).isEqualTo("저장된 뉴스 1");
        assertThat(result.get(4).getTitle()).isEqualTo("저장된 뉴스 5");
        
        verify(cryptoNewsRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("오래된 뉴스 정리 테스트 - 삭제할 것이 있는 경우")
    void cleanupOldNewsWithDeletion() {
        // given
        int maxNewsCount = 10;
        long totalCount = 15; // 15개가 있고 10개만 유지 -> 5개 삭제
        
        given(cryptoNewsRepository.count()).willReturn(totalCount);
        
        List<CryptoNews> oldestNews = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            oldestNews.add(createTestCryptoNews((long) i, "오래된 뉴스 " + i));
        }
        
        given(cryptoNewsRepository.findOldestNews(any(Pageable.class))).willReturn(oldestNews);

        // when
        int deletedCount = cryptoNewsService.cleanupOldNews(maxNewsCount);

        // then
        assertThat(deletedCount).isEqualTo(5);
        
        verify(cryptoNewsRepository, times(1)).count();
        verify(cryptoNewsRepository, times(1)).findOldestNews(any(Pageable.class));
        
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cryptoNewsRepository, times(1)).deleteAllByIdInBatch(idsCaptor.capture());
        
        List<Long> deletedIds = idsCaptor.getValue();
        assertThat(deletedIds).hasSize(5);
        assertThat(deletedIds).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("오래된 뉴스 정리 테스트 - 삭제할 것이 없는 경우")
    void cleanupOldNewsWithoutDeletion() {
        // given
        int maxNewsCount = 10;
        long totalCount = 8; // 8개가 있고 10개 유지 -> 삭제 없음
        
        given(cryptoNewsRepository.count()).willReturn(totalCount);

        // when
        int deletedCount = cryptoNewsService.cleanupOldNews(maxNewsCount);

        // then
        assertThat(deletedCount).isEqualTo(0);
        
        verify(cryptoNewsRepository, times(1)).count();
        verify(cryptoNewsRepository, never()).findOldestNews(any(Pageable.class));
        verify(cryptoNewsRepository, never()).deleteAllByIdInBatch(anyList());
    }

    @Test
    @DisplayName("오래된 뉴스 정리 테스트 - 대량 삭제 제한")
    void cleanupOldNewsWithLargeNumberLimitation() {
        // given
        int maxNewsCount = 10;
        long totalCount = 1000; // 1000개가 있고 10개 유지 -> 990개 삭제 대상이지만 최대 100개만 삭제
        
        given(cryptoNewsRepository.count()).willReturn(totalCount);
        
        List<CryptoNews> oldestNews = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            oldestNews.add(createTestCryptoNews((long) i, "오래된 뉴스 " + i));
        }
        
        given(cryptoNewsRepository.findOldestNews(any(Pageable.class))).willReturn(oldestNews);

        // when
        int deletedCount = cryptoNewsService.cleanupOldNews(maxNewsCount);

        // then
        assertThat(deletedCount).isEqualTo(100); // 최대 100개만 삭제
        
        verify(cryptoNewsRepository, times(1)).count();
        verify(cryptoNewsRepository, times(1)).findOldestNews(any(Pageable.class));
        
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cryptoNewsRepository).findOldestNews(pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isEqualTo(100);
        
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cryptoNewsRepository, times(1)).deleteAllByIdInBatch(idsCaptor.capture());
        
        List<Long> deletedIds = idsCaptor.getValue();
        assertThat(deletedIds).hasSize(100);
    }

    private CryptoNews createTestCryptoNews(Long id, String title) {
        return CryptoNews.builder()
                .id(id)
                .title(title)
                .link("https://example.com/news-" + id)
                .source("테스트 소스")
                .date(LocalDateTime.now().toString())
                .thumbnail("https://example.com/thumbnail-" + id + ".jpg")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Object> createMockApiResponseMap() {
        List<Map<String, Object>> newsResults = new ArrayList<>();
        
        Map<String, Object> news1 = new HashMap<>();
        news1.put("title", "뉴스 제목 1");
        news1.put("link", "https://example.com/news-1");
        news1.put("source", "뉴스 소스 1");
        news1.put("date", "2023-05-20");
        news1.put("thumbnail", "https://example.com/thumbnail-1.jpg");
        newsResults.add(news1);
        
        Map<String, Object> news2 = new HashMap<>();
        news2.put("title", "뉴스 제목 2");
        news2.put("link", "https://example.com/news-2");
        news2.put("source", "뉴스 소스 2");
        news2.put("date", "2023-05-19");
        news2.put("thumbnail", "https://example.com/thumbnail-2.jpg");
        newsResults.add(news2);
        
        Map<String, Object> response = new HashMap<>();
        response.put("news_results", newsResults);
        
        return response;
    }
}
