package com.autocoin.news.integration;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestEntityConfig;
import com.autocoin.config.TestSchedulingConfig;
import com.autocoin.config.TestWebConfig;
import com.autocoin.config.TestJwtConfig;
import com.autocoin.news.config.NewsApiConfig;
import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.dto.CryptoNewsDto;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // application-test.yml 설정 파일 사용
@Import({TestConfig.class, TestEntityConfig.class, TestSchedulingConfig.class, TestWebConfig.class, TestJwtConfig.class})
public class NewsTestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CryptoNewsRepository cryptoNewsRepository;

    @MockBean
    private RestTemplate restTemplate; // 실제 API 호출 방지

    @MockBean
    private NewsApiConfig newsApiConfig; // API URL 모킹

    @BeforeEach
    void setUp() {
        // 테스트용 뉴스 데이터 초기화
        cryptoNewsRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        cryptoNewsRepository.deleteAll();
    }

    @Test
    @DisplayName("테스트 뉴스 추가 통합 테스트")
    void addTestNewsIntegration() throws Exception {
        // when
        MvcResult result = mockMvc.perform(post("/api/v1/news/test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String content = result.getResponse().getContentAsString();
        CryptoNewsDto responseDto = objectMapper.readValue(content, CryptoNewsDto.class);
        
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getTitle()).isEqualTo("영호쿠폰 - 테스트 뉴스");
        
        // DB에 실제로 저장되었는지 확인
        List<CryptoNews> savedNews = cryptoNewsRepository.findAll();
        assertThat(savedNews).hasSize(1);
        assertThat(savedNews.get(0).getTitle()).isEqualTo("영호쿠폰 - 테스트 뉴스");
    }

    @Test
    @DisplayName("커스텀 테스트 뉴스 추가 통합 테스트")
    void addCustomTestNewsIntegration() throws Exception {
        // given
        String customTitle = "커스텀 통합 테스트 뉴스";

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/news/test/custom")
                .param("title", customTitle)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String content = result.getResponse().getContentAsString();
        CryptoNewsDto responseDto = objectMapper.readValue(content, CryptoNewsDto.class);
        
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getTitle()).isEqualTo(customTitle);
        
        // DB에 실제로 저장되었는지 확인
        List<CryptoNews> savedNews = cryptoNewsRepository.findAll();
        assertThat(savedNews).hasSize(1);
        assertThat(savedNews.get(0).getTitle()).isEqualTo(customTitle);
    }

    @Test
    @DisplayName("SerpAPI 뉴스 가져오기 통합 테스트")
    void fetchNewsFromApiIntegration() throws Exception {
        // given
        String apiUrl = "https://serpapi.com/search.json?q=crypto+news&tbm=nws&api_key=test-key";
        String mockApiResponse = createMockApiResponse();
        
        given(newsApiConfig.getSerpApiUrl()).willReturn(apiUrl);
        given(restTemplate.getForObject(eq(apiUrl), eq(String.class))).willReturn(mockApiResponse);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/news/test/fetch-from-api")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String content = result.getResponse().getContentAsString();
        List<CryptoNewsDto> responseDtos = objectMapper.readValue(content, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, CryptoNewsDto.class));
        
        assertThat(responseDtos).isNotNull();
        assertThat(responseDtos).hasSize(2);
        assertThat(responseDtos.get(0).getTitle()).isEqualTo("뉴스 제목 1");
        assertThat(responseDtos.get(1).getTitle()).isEqualTo("뉴스 제목 2");
        
        // DB에 실제로 저장되었는지 확인
        List<CryptoNews> savedNews = cryptoNewsRepository.findAll();
        assertThat(savedNews).hasSize(2);
        
        // 링크로 정렬하여 확인
        List<String> titlesSorted = savedNews.stream()
                .map(CryptoNews::getTitle)
                .sorted()
                .toList();
        assertThat(titlesSorted).containsExactly("뉴스 제목 1", "뉴스 제목 2");
    }

    @Test
    @DisplayName("저장된 전체 뉴스 조회 통합 테스트")
    void getAllSavedNewsIntegration() throws Exception {
        // given
        // 테스트 뉴스 저장
        for (int i = 1; i <= 5; i++) {
            cryptoNewsRepository.save(CryptoNews.builder()
                    .title("통합 테스트 뉴스 " + i)
                    .link("https://example.com/integration-test-" + i)
                    .source("통합 테스트 소스")
                    .date(LocalDateTime.now().toString())
                    .thumbnail("https://example.com/integration-thumbnail-" + i + ".jpg")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/news/test/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String content = result.getResponse().getContentAsString();
        List<CryptoNewsDto> responseDtos = objectMapper.readValue(content, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, CryptoNewsDto.class));
        
        assertThat(responseDtos).isNotNull();
        assertThat(responseDtos).hasSize(5);
        
        // 제목 확인
        List<String> titles = responseDtos.stream()
                .map(CryptoNewsDto::getTitle)
                .toList();
        
        for (int i = 1; i <= 5; i++) {
            assertThat(titles).contains("통합 테스트 뉴스 " + i);
        }
    }

    private String createMockApiResponse() {
        return "{" +
                "\"news_results\": [" +
                "  {" +
                "    \"title\": \"뉴스 제목 1\"," +
                "    \"link\": \"https://example.com/news-1\"," +
                "    \"source\": \"뉴스 소스 1\"," +
                "    \"date\": \"2023-05-20\"," +
                "    \"thumbnail\": \"https://example.com/thumbnail-1.jpg\"" +
                "  }," +
                "  {" +
                "    \"title\": \"뉴스 제목 2\"," +
                "    \"link\": \"https://example.com/news-2\"," +
                "    \"source\": \"뉴스 소스 2\"," +
                "    \"date\": \"2023-05-19\"," +
                "    \"thumbnail\": \"https://example.com/thumbnail-2.jpg\"" +
                "  }" +
                "]" +
                "}";
    }
}
