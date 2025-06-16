package com.autocoin.post.domain;

import com.autocoin.post.domain.entity.Post;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.Role;
import com.autocoin.category.domain.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 도메인 엔티티 테스트
 */
class PostTest {

    @Test
    @DisplayName("Post 생성 테스트")
    void createPost() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        Category category = Category.builder()
                .id(1L)
                .name("테스트 카테고리")
                .description("테스트 설명")
                .build();

        // when
        Post post = Post.builder()
                .id(1L)
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .user(user)
                .category(category)
                .build();

        // then
        assertThat(post.getId()).isEqualTo(1L);
        assertThat(post.getTitle()).isEqualTo("테스트 제목");
        assertThat(post.getContent()).isEqualTo("테스트 내용");
        assertThat(post.getWriter()).isEqualTo("테스트 작성자");
        assertThat(post.getUser()).isEqualTo(user);
        assertThat(post.getCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("Post 업데이트 테스트")
    void updatePost() {
        // given
        Post post = Post.builder()
                .id(1L)
                .title("원본 제목")
                .content("원본 내용")
                .writer("원본 작성자")
                .build();

        // when
        post.update("수정된 제목", "수정된 내용", null, null, null, "수정된 작성자", null);

        // then
        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");
        assertThat(post.getWriter()).isEqualTo("수정된 작성자");
    }
}
