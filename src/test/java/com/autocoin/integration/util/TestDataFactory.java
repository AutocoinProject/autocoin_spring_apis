package com.autocoin.integration.util;

import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.dto.UserLoginRequestDto;
import com.autocoin.user.dto.UserSignupRequestDto;
import com.autocoin.post.domain.entity.Post;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.news.domain.entity.CryptoNews;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

/**
 * 통합 테스트에서 사용할 테스트 데이터 생성 유틸리티
 */
public class TestDataFactory {

    /**
     * 테스트용 사용자 생성
     */
    public static User createTestUser(String email, String username, String password) {
        return User.builder()
                .email(email)
                .username(username)
                .password(password)
                .role(Role.USER)
                .build();
    }

    /**
     * 테스트용 관리자 사용자 생성
     */
    public static User createTestAdmin(String email, String username, String password) {
        return User.builder()
                .email(email)
                .username(username)
                .password(password)
                .role(Role.ADMIN)
                .build();
    }

    /**
     * 회원가입 요청 DTO 생성
     */
    public static UserSignupRequestDto createSignupRequest(String email, String username, String password) {
        return UserSignupRequestDto.builder()
                .email(email)
                .username(username)
                .password(password)
                .build();
    }

    /**
     * 로그인 요청 DTO 생성
     */
    public static UserLoginRequestDto createLoginRequest(String email, String password) {
        return UserLoginRequestDto.builder()
                .email(email)
                .password(password)
                .build();
    }

    /**
     * 테스트용 게시글 엔티티 생성
     */
    public static Post createTestPost(String title, String content, String writer) {
        return Post.builder()
                .title(title)
                .content(content)
                .writer(writer)
                .build();
    }

    /**
     * 테스트용 카테고리 엔티티 생성
     */
    public static Category createTestCategory(String name, String description) {
        return Category.builder()
                .name(name)
                .description(description)
                .build();
    }

    /**
     * 테스트용 MockMultipartFile 생성
     */
    public static MockMultipartFile createTestImageFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "image/jpeg",
                content.getBytes()
        );
    }

    /**
     * 빈 파일 생성 (파일 없는 테스트용)
     */
    public static MockMultipartFile createEmptyFile() {
        return new MockMultipartFile(
                "file",
                "",
                "application/octet-stream",
                new byte[0]
        );
    }

    /**
     * 기본 테스트 이메일 생성
     */
    public static String createTestEmail(String prefix) {
        return prefix + "@test.example.com";
    }

    /**
     * 기본 테스트 비밀번호 생성
     */
    public static String createTestPassword() {
        return "TestPassword123!";
    }

    /**
     * 테스트용 암호화폐 뉴스 엔티티 생성
     */
    public static CryptoNews createTestCryptoNews(String title, String link, String source) {
        return CryptoNews.builder()
                .title(title)
                .link(link)
                .source(source)
                .date(LocalDateTime.now().toString())
                .thumbnail("https://example.com/test-thumbnail.jpg")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기본 테스트용 암호화폐 뉴스 생성
     */
    public static CryptoNews createDefaultTestCryptoNews() {
        return createTestCryptoNews(
                "테스트 뉴스 제목",
                "https://example.com/test-news",
                "테스트 소스"
        );
    }
}
