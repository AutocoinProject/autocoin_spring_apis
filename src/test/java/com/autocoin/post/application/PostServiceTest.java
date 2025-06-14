package com.autocoin.post.application;

import com.autocoin.category.application.service.CategoryService;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.file.application.service.S3UploaderInterface;
import com.autocoin.post.application.service.PostService;
import com.autocoin.post.domain.PostRepository;
import com.autocoin.post.domain.entity.Post;
import com.autocoin.post.dto.request.PostRequestDto;
import com.autocoin.post.dto.response.PostResponseDto;
import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * PostService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private S3UploaderInterface s3UploaderInterface;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        testCategory = Category.builder()
                .id(1L)
                .name("테스트 카테고리")
                .description("테스트 설명")
                .build();

        // CategoryService Mock 설정
        given(categoryService.getDefaultCategory()).willReturn(testCategory);
    }

    @Test
    @DisplayName("게시글 생성 테스트 - 파일 없음")
    void createPost_WithoutFile() {
        // given
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .build();

        Post savedPost = Post.builder()
                .id(1L)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .writer(requestDto.getWriter())
                .user(testUser)
                .category(testCategory)
                .build();

        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponseDto result = postService.createPost(requestDto, testUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 제목");
        assertThat(result.getContent()).isEqualTo("테스트 내용");
        assertThat(result.getWriter()).isEqualTo("테스트 작성자");

        verify(postRepository, times(1)).save(any(Post.class));
    }
}
