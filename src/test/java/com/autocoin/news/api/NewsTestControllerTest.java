package com.autocoin.news.api;

import com.autocoin.news.api.NewsTestController; // 올바른 패키지 경로
import com.autocoin.news.config.NewsApiConfig;
import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import com.autocoin.news.application.service.CryptoNewsService;
import com.autocoin.news.infrastructure.external.SerpApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsTestController.class)
@ActiveProfiles("test")
public class NewsTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CryptoNewsRepository cryptoNewsRepository;

    @MockBean
    private CryptoNewsService cryptoNewsService;

    @MockBean
    private SerpApiClient serpApiClient;

    @MockBean
    private NewsApiConfig newsApiConfig;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("테스트 뉴스 추가 성공 테스트")
    void addTestNewsSuccess() throws Exception {
        // given
        CryptoNews savedNews = createTestCryptoNews(1L, "영호쿠폰 - 테스트 뉴스");
        given(cryptoNewsRepository.save(any(CryptoNews.class))).willReturn(savedNews);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/v1/news/test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("영호쿠폰 - 테스트 뉴스"))
                .andReturn();

        // 응답 내용 검증
        String content = result.getResponse().getContentAsString();
        CryptoNewsDto responseDto = objectMapper.readValue(content, CryptoNewsDto.class);
        
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(1L);
        assertThat(responseDto.getTitle()).isEqualTo("영호쿠폰 - 테스트 뉴스");
        
        verify(cryptoNewsRepository, times(1)).save(any(CryptoNews.class));
    }

    @Test
    @DisplayName("SerpAPI 뉴스 가져오기 성공 테스트")
    void fetchNewsFromApiSuccess() throws Exception {
        // given
        Map<String, Object> mockApiResponse = createMockApiResponseMap();
        given(serpApiClient.fetchCryptoNews()).willReturn(mockApiResponse);
        given(cryptoNewsRepository.existsByLink(anyString())).willReturn(false);
        given(cryptoNewsRepository.save(any(CryptoNews.class))).willAnswer(invocation -> {
            CryptoNews news = invocation.getArgument(0);
            return CryptoNews.builder()
                    .id(3L)
                    .title(news.getTitle())
                    .link(news.getLink())
                    .source(news.getSource())
                    .date(news.getDate())
                    .thumbnail(news.getThumbnail())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        // when & then
        MvcResult result = mockMvc.perform(get("/api/v1/news/test/fetch-from-api")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("뉴스 제목 1"))
                .andExpect(jsonPath("$[1].title").value("뉴스 제목 2"))
                .andReturn();

        // 응답 내용 검증
        String content = result.getResponse().getContentAsString();
        TypeReference<List<CryptoNewsDto>> typeRef = new TypeReference<List<CryptoNewsDto>>() {};
        List<CryptoNewsDto> responseDtos = objectMapper.readValue(content, typeRef);
        
        assertThat(responseDtos).isNotNull();
        assertThat(responseDtos).hasSize(2);
        assertThat(responseDtos.get(0).getTitle()).isEqualTo("뉴스 제목 1");
        assertThat(responseDtos.get(1).getTitle()).isEqualTo("뉴스 제목 2");
        
        verify(serpApiClient, times(1)).fetchCryptoNews();
        verify(cryptoNewsRepository, times(2)).existsByLink(anyString());
        verify(cryptoNewsRepository, times(2)).save(any(CryptoNews.class));
    }

    @Test
    @DisplayName("저장된 전체 뉴스 조회 성공 테스트")
    void getAllSavedNewsSuccess() throws Exception {
        // given
        List<CryptoNews> savedNewsList = Arrays.asList(
                createTestCryptoNews(1L, "저장된 뉴스 1"),
                createTestCryptoNews(2L, "저장된 뉴스 2"),
                createTestCryptoNews(3L, "저장된 뉴스 3")
        );
        
        given(cryptoNewsRepository.findAll()).willReturn(savedNewsList);

        // when & then
        MvcResult result = mockMvc.perform(get("/api/v1/news/test/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("저장된 뉴스 1"))
                .andExpect(jsonPath("$[1].title").value("저장된 뉴스 2"))
                .andExpect(jsonPath("$[2].title").value("저장된 뉴스 3"))
                .andReturn();

        // 응답 내용 검증
        String content = result.getResponse().getContentAsString();
        TypeReference<List<CryptoNewsDto>> typeRef = new TypeReference<List<CryptoNewsDto>>() {};
        List<CryptoNewsDto> responseDtos = objectMapper.readValue(content, typeRef);
        
        assertThat(responseDtos).isNotNull();
        assertThat(responseDtos).hasSize(3);
        assertThat(responseDtos.get(0).getTitle()).isEqualTo("저장된 뉴스 1");
        assertThat(responseDtos.get(1).getTitle()).isEqualTo("저장된 뉴스 2");
        assertThat(responseDtos.get(2).getTitle()).isEqualTo("저장된 뉴스 3");
        
        verify(cryptoNewsRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("API 호출 실패 테스트")
    void fetchNewsFromApiFailure() throws Exception {
        // given
        given(serpApiClient.fetchCryptoNews())
                .willThrow(new RuntimeException("API 호출 실패"));

        // when & then
        mockMvc.perform(get("/api/v1/news/test/fetch-from-api")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(serpApiClient, times(1)).fetchCryptoNews();
    }

    @Test
    @DisplayName("빈 API 응답 처리 테스트")
    void fetchNewsFromApiEmptyResponse() throws Exception {
        // given
        Map<String, Object> emptyResponse = new HashMap<>();
        given(serpApiClient.fetchCryptoNews()).willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/api/v1/news/test/fetch-from-api")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        
        verify(serpApiClient, times(1)).fetchCryptoNews();
    }

    @Test
    @DisplayName("중복 링크 뉴스 저장 방지 테스트")
    void preventDuplicateNewsSave() throws Exception {
        // given
        Map<String, Object> mockApiResponse = createMockApiResponseMap();
        given(serpApiClient.fetchCryptoNews()).willReturn(mockApiResponse);
        given(cryptoNewsRepository.existsByLink("https://example.com/news-1")).willReturn(true); // 첫 번째는 중복
        given(cryptoNewsRepository.existsByLink("https://example.com/news-2")).willReturn(false); // 두 번째는 새로운 뉴스
        
        given(cryptoNewsRepository.save(any(CryptoNews.class))).willAnswer(invocation -> {
            CryptoNews news = invocation.getArgument(0);
            return CryptoNews.builder()
                    .id(4L)
                    .title(news.getTitle())
                    .link(news.getLink())
                    .source(news.getSource())
                    .date(news.getDate())
                    .thumbnail(news.getThumbnail())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        // when & then
        mockMvc.perform(get("/api/v1/news/test/fetch-from-api")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(serpApiClient, times(1)).fetchCryptoNews();
        verify(cryptoNewsRepository, times(2)).existsByLink(anyString());
        verify(cryptoNewsRepository, times(1)).save(any(CryptoNews.class)); // 하나만 저장됨
    }

    // 유틸리티 메서드
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
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> newsResults = Arrays.asList(
                createNewsItemMap("뉴스 제목 1", "https://example.com/news-1", "뉴스 소스 1", "2023-05-20", "https://example.com/thumbnail-1.jpg"),
                createNewsItemMap("뉴스 제목 2", "https://example.com/news-2", "뉴스 소스 2", "2023-05-19", "https://example.com/thumbnail-2.jpg")
        );
        response.put("news_results", newsResults);
        return response;
    }

    private Map<String, Object> createNewsItemMap(String title, String link, String source, String date, String thumbnail) {
        Map<String, Object> newsItem = new HashMap<>();
        newsItem.put("title", title);
        newsItem.put("link", link);
        newsItem.put("source", source);
        newsItem.put("date", date);
        newsItem.put("thumbnail", thumbnail);
        return newsItem;
    }
}
