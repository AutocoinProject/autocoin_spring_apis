package com.autocoin.news.repository;

import com.autocoin.news.domain.entity.CryptoNews;
import com.autocoin.news.infrastructure.repository.CryptoNewsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // application-test.yml 설정 파일 사용
public class CryptoNewsRepositoryTest {

    @Autowired
    private CryptoNewsRepository cryptoNewsRepository;

    @BeforeEach
    void setUp() {
        // 테스트 전 모든 데이터 삭제
        cryptoNewsRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 모든 데이터 삭제
        cryptoNewsRepository.deleteAll();
    }

    @Test
    @DisplayName("뉴스 저장 및 조회 테스트")
    void saveAndFindNewsTest() {
        // given
        CryptoNews news = CryptoNews.builder()
                .title("테스트 뉴스 제목")
                .link("https://example.com/test-news")
                .source("테스트 소스")
                .date(LocalDateTime.now().toString())
                .thumbnail("https://example.com/test-thumbnail.jpg")
                .createdAt(LocalDateTime.now())
                .build();

        // when
        CryptoNews savedNews = cryptoNewsRepository.save(news);
        Optional<CryptoNews> foundNews = cryptoNewsRepository.findById(savedNews.getId());

        // then
        assertThat(foundNews).isPresent();
        assertThat(foundNews.get().getTitle()).isEqualTo("테스트 뉴스 제목");
        assertThat(foundNews.get().getLink()).isEqualTo("https://example.com/test-news");
    }

    @Test
    @DisplayName("링크로 뉴스 조회 테스트")
    void findNewsByLinkTest() {
        // given
        String testLink = "https://example.com/unique-test-link";
        CryptoNews news = CryptoNews.builder()
                .title("테스트 뉴스 제목")
                .link(testLink)
                .source("테스트 소스")
                .date(LocalDateTime.now().toString())
                .thumbnail("https://example.com/test-thumbnail.jpg")
                .createdAt(LocalDateTime.now())
                .build();
        
        cryptoNewsRepository.save(news);

        // when
        Optional<CryptoNews> foundNews = cryptoNewsRepository.findByLink(testLink);

        // then
        assertThat(foundNews).isPresent();
        assertThat(foundNews.get().getTitle()).isEqualTo("테스트 뉴스 제목");
    }

    @Test
    @DisplayName("링크 존재 여부 확인 테스트")
    void existsByLinkTest() {
        // given
        String testLink = "https://example.com/exists-test-link";
        CryptoNews news = CryptoNews.builder()
                .title("테스트 뉴스 제목")
                .link(testLink)
                .source("테스트 소스")
                .date(LocalDateTime.now().toString())
                .thumbnail("https://example.com/test-thumbnail.jpg")
                .createdAt(LocalDateTime.now())
                .build();
        
        cryptoNewsRepository.save(news);

        // when
        boolean exists = cryptoNewsRepository.existsByLink(testLink);
        boolean notExists = cryptoNewsRepository.existsByLink("https://example.com/non-existent-link");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("가장 오래된 뉴스 조회 테스트")
    void findOldestNewsTest() {
        // given
        // 1. 오래된 뉴스 (3일 전)
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        for (int i = 1; i <= 3; i++) {
            CryptoNews oldNews = CryptoNews.builder()
                    .title("오래된 뉴스 " + i)
                    .link("https://example.com/old-news-" + i)
                    .source("테스트 소스")
                    .date(threeDaysAgo.toString())
                    .thumbnail("https://example.com/old-thumbnail-" + i + ".jpg")
                    .createdAt(threeDaysAgo)
                    .build();
            
            cryptoNewsRepository.save(oldNews);
        }
        
        // 2. 최신 뉴스 (현재)
        for (int i = 1; i <= 3; i++) {
            CryptoNews recentNews = CryptoNews.builder()
                    .title("최신 뉴스 " + i)
                    .link("https://example.com/recent-news-" + i)
                    .source("테스트 소스")
                    .date(LocalDateTime.now().toString())
                    .thumbnail("https://example.com/recent-thumbnail-" + i + ".jpg")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            cryptoNewsRepository.save(recentNews);
        }

        // when
        Pageable pageable = PageRequest.of(0, 2); // 가장 오래된 뉴스 2개만 조회
        List<CryptoNews> oldestNews = cryptoNewsRepository.findOldestNews(pageable);

        // then
        assertThat(oldestNews).hasSize(2);
        
        // 생성일 기준 오름차순 정렬이므로 첫 번째 결과는 가장 오래된 뉴스
        assertThat(oldestNews.get(0).getTitle()).startsWith("오래된 뉴스");
        assertThat(oldestNews.get(1).getTitle()).startsWith("오래된 뉴스");
        
        // 생성일 검증
        assertThat(oldestNews.get(0).getCreatedAt()).isBefore(LocalDateTime.now().minusDays(2));
    }
}
