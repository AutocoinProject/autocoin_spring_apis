package com.autocoin.post.infrastructure;

import com.autocoin.post.domain.entity.Post;
import com.autocoin.post.infrastructure.repository.PostJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Test
    @DisplayName("게시글 저장 테스트")
    void savePost() {
        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .build();

        // when
        Post savedPost = postJpaRepository.save(post);

        // then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo(post.getTitle());
        assertThat(savedPost.getContent()).isEqualTo(post.getContent());
        assertThat(savedPost.getWriter()).isEqualTo(post.getWriter());
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ID로 게시글 조회 테스트")
    void findPostById() {
        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .build();

        Post savedPost = postJpaRepository.save(post);

        // when
        Optional<Post> foundPost = postJpaRepository.findById(savedPost.getId());

        // then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getId()).isEqualTo(savedPost.getId());
        assertThat(foundPost.get().getTitle()).isEqualTo(savedPost.getTitle());
        assertThat(foundPost.get().getContent()).isEqualTo(savedPost.getContent());
        assertThat(foundPost.get().getWriter()).isEqualTo(savedPost.getWriter());
    }

    @Test
    @DisplayName("모든 게시글 작성일 내림차순 조회 테스트")
    void findAllByOrderByCreatedAtDesc() {
        // given
        Post post1 = Post.builder()
                .title("제목 1")
                .content("내용 1")
                .writer("작성자 1")
                .build();

        Post post2 = Post.builder()
                .title("제목 2")
                .content("내용 2")
                .writer("작성자 2")
                .build();

        postJpaRepository.save(post1);
        // 시간 차이를 주기 위한 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        postJpaRepository.save(post2);

        // when
        List<Post> posts = postJpaRepository.findAllByOrderByCreatedAtDesc();

        // then
        assertThat(posts).isNotEmpty();
        assertThat(posts).hasSize(2);
        // 최신 게시글부터 정렬되어야 함
        assertThat(posts.get(0).getTitle()).isEqualTo("제목 2");
        assertThat(posts.get(1).getTitle()).isEqualTo("제목 1");
    }

    @Test
    @DisplayName("게시글 삭제 테스트")
    void deletePost() {
        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .build();

        Post savedPost = postJpaRepository.save(post);
        
        // when
        postJpaRepository.delete(savedPost);
        Optional<Post> deletedPost = postJpaRepository.findById(savedPost.getId());
        
        // then
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("게시글 업데이트 테스트")
    void updatePost() {
        // given
        Post post = Post.builder()
                .title("원본 제목")
                .content("원본 내용")
                .writer("원본 작성자")
                .build();

        Post savedPost = postJpaRepository.save(post);
        
        // when
        savedPost.update("수정된 제목", "수정된 내용", null, null, null, "원본 작성자", null);
        Post updatedPost = postJpaRepository.save(savedPost);
        
        // then
        assertThat(updatedPost.getId()).isEqualTo(savedPost.getId());
        assertThat(updatedPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.getContent()).isEqualTo("수정된 내용");
        assertThat(updatedPost.getWriter()).isEqualTo("원본 작성자");
    }
}
