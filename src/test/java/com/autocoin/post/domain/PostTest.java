package com.autocoin.post.domain;

import com.autocoin.post.domain.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("Post 엔티티 생성 테스트")
    void createPost() {
        // given
        String title = "테스트 제목";
        String content = "테스트 내용";
        String writer = "테스트 작성자";
        String fileUrl = "https://example.com/test.jpg";
        String fileName = "test.jpg";
        String fileKey = "posts/test.jpg";

        // when
        Post post = Post.builder()
                .title(title)
                .content(content)
                .writer(writer)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileKey(fileKey)
                .build();

        // then
        assertThat(post).isNotNull();
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getWriter()).isEqualTo(writer);
        assertThat(post.getFileUrl()).isEqualTo(fileUrl);
        assertThat(post.getFileName()).isEqualTo(fileName);
        assertThat(post.getFileKey()).isEqualTo(fileKey);
    }

    @Test
    @DisplayName("Post 엔티티 수정 테스트")
    void updatePost() {
        // given
        Post post = Post.builder()
                .title("원본 제목")
                .content("원본 내용")
                .writer("원본 작성자")
                .fileUrl("https://example.com/original.jpg")
                .fileName("original.jpg")
                .fileKey("posts/original.jpg")
                .build();

        String newTitle = "수정된 제목";
        String newContent = "수정된 내용";
        String newFileUrl = "https://example.com/updated.jpg";
        String newFileName = "updated.jpg";
        String newFileKey = "posts/updated.jpg";

        // when
        post.update(newTitle, newContent, newFileUrl, newFileName, newFileKey, post.getWriter(), null);

        // then
        assertThat(post.getTitle()).isEqualTo(newTitle);
        assertThat(post.getContent()).isEqualTo(newContent);
        assertThat(post.getFileUrl()).isEqualTo(newFileUrl);
        assertThat(post.getFileName()).isEqualTo(newFileName);
        assertThat(post.getFileKey()).isEqualTo(newFileKey);
    }

    @Test
    @DisplayName("Post 엔티티 수정 시 파일 정보가 null이면 기존 파일 정보 유지")
    void updatePostWithNullFile() {
        // given
        String originalFileUrl = "https://example.com/original.jpg";
        String originalFileName = "original.jpg";
        String originalFileKey = "posts/original.jpg";
        
        Post post = Post.builder()
                .title("원본 제목")
                .content("원본 내용")
                .writer("원본 작성자")
                .fileUrl(originalFileUrl)
                .fileName(originalFileName)
                .fileKey(originalFileKey)
                .build();

        String newTitle = "수정된 제목";
        String newContent = "수정된 내용";

        // when
        post.update(newTitle, newContent, null, null, null, post.getWriter(), null);

        // then
        assertThat(post.getTitle()).isEqualTo(newTitle);
        assertThat(post.getContent()).isEqualTo(newContent);
        assertThat(post.getFileUrl()).isEqualTo(originalFileUrl);
        assertThat(post.getFileName()).isEqualTo(originalFileName);
        assertThat(post.getFileKey()).isEqualTo(originalFileKey);
    }
}