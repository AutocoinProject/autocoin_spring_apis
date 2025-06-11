package com.autocoin.post.application;

import com.autocoin.file.application.service.S3Uploader;
import com.autocoin.global.exception.business.ResourceNotFoundException;
import com.autocoin.post.application.service.PostService;
import com.autocoin.post.domain.PostRepository;
import com.autocoin.post.domain.entity.Post;
import com.autocoin.post.dto.request.PostRequestDto;
import com.autocoin.post.dto.response.PostResponseDto;
import com.autocoin.category.application.service.CategoryService;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.category.dto.response.CategoryResponseDto;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private S3Uploader s3Uploader; // S3UploaderInterface 대신 구체 클래스 사용

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private Category defaultCategory;
    private CategoryResponseDto defaultCategoryDto;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .password("encoded_password")
                .role(Role.USER)
                .build();

        // 테스트용 카테고리 생성
        defaultCategory = Category.builder()
                .id(1L)
                .name("기본 카테고리")
                .description("기본 카테고리 설명")
                .build();

        defaultCategoryDto = CategoryResponseDto.builder()
                .id(1L)
                .name("기본 카테고리")
                .description("기본 카테고리 설명")
                .build();

        // 모든 테스트에서 기본 카테고리가 반환되도록 설정
        given(categoryService.getDefaultCategory()).willReturn(defaultCategory);
    }

    @Test
    @DisplayName("게시글 생성 성공 테스트 (파일 없음)")
    void createPostWithoutFile() {
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
                .category(defaultCategory)
                .build();

        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponseDto responseDto = postService.createPost(requestDto, testUser);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(savedPost.getId());
        assertThat(responseDto.getTitle()).isEqualTo(savedPost.getTitle());
        assertThat(responseDto.getContent()).isEqualTo(savedPost.getContent());
        assertThat(responseDto.getWriter()).isEqualTo(savedPost.getWriter());
        assertThat(responseDto.getFileUrl()).isNull();
        
        verify(postRepository, times(1)).save(any(Post.class));
        verify(s3Uploader, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("게시글 생성 성공 테스트 (파일 포함)")
    void createPostWithFile() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        PostRequestDto requestDto = PostRequestDto.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .file(file)  // RequestDto에 파일 포함
                .build();

        String fileUrl = "https://example.com/posts/test.jpg";
        given(s3Uploader.upload(any(MultipartFile.class), eq("posts"))).willReturn(fileUrl);

        Post savedPost = Post.builder()
                .id(1L)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .writer(requestDto.getWriter())
                .user(testUser)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileKey("posts/test.jpg")
                .category(defaultCategory)
                .build();

        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponseDto responseDto = postService.createPost(requestDto, testUser);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(savedPost.getId());
        assertThat(responseDto.getTitle()).isEqualTo(savedPost.getTitle());
        assertThat(responseDto.getContent()).isEqualTo(savedPost.getContent());
        assertThat(responseDto.getWriter()).isEqualTo(savedPost.getWriter());
        assertThat(responseDto.getFileUrl()).isEqualTo(fileUrl);
        assertThat(responseDto.getFileName()).isEqualTo(file.getOriginalFilename());
        
        verify(postRepository, times(1)).save(any(Post.class));
        verify(s3Uploader, times(1)).upload(any(MultipartFile.class), eq("posts"));
    }

    @Test
    @DisplayName("게시글 생성 성공 테스트 (사용자 없음 - 익명)")
    void createPostWithoutUser() {
        // given
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("익명 게시글")
                .content("익명 사용자 내용")
                .writer("익명")
                .build();

        Post savedPost = Post.builder()
                .id(1L)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .writer(requestDto.getWriter())
                .category(defaultCategory)
                // user는 null
                .build();

        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponseDto responseDto = postService.createPost(requestDto, null);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(savedPost.getId());
        assertThat(responseDto.getTitle()).isEqualTo(savedPost.getTitle());
        assertThat(responseDto.getWriter()).isEqualTo("익명");
        
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 성공 테스트 (S3 업로드 실패해도 게시글 작성)")
    void createPostWithFileUploadFailure() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        PostRequestDto requestDto = PostRequestDto.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .file(file)
                .build();

        // S3 업로드 실패 시뮬레이션
        given(s3Uploader.upload(any(MultipartFile.class), eq("posts")))
                .willThrow(new RuntimeException("S3 connection failed"));

        Post savedPost = Post.builder()
                .id(1L)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .writer(requestDto.getWriter())
                .user(testUser)
                .category(defaultCategory)
                // 파일 관련 필드는 null
                .build();

        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponseDto responseDto = postService.createPost(requestDto, testUser);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(savedPost.getId());
        assertThat(responseDto.getTitle()).isEqualTo(savedPost.getTitle());
        assertThat(responseDto.getFileUrl()).isNull(); // 파일 업로드 실패로 null
        
        verify(postRepository, times(1)).save(any(Post.class));
        verify(s3Uploader, times(1)).upload(any(MultipartFile.class), eq("posts"));
    }

    @Test
    @DisplayName("ID로 게시글 조회 성공 테스트")
    void getPostSuccess() {
        // given
        Long postId = 1L;
        Post post = Post.builder()
                .id(postId)
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .user(testUser)
                .category(defaultCategory)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        PostResponseDto responseDto = postService.getPost(postId);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(post.getId());
        assertThat(responseDto.getTitle()).isEqualTo(post.getTitle());
        assertThat(responseDto.getContent()).isEqualTo(post.getContent());
        assertThat(responseDto.getWriter()).isEqualTo(post.getWriter());
        
        verify(postRepository, times(1)).findById(postId);
    }

    @Test
    @DisplayName("ID로 게시글 조회 실패 테스트 - 존재하지 않는 게시글")
    void getPostFail() {
        // given
        Long postId = 99L;
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("해당 ID의 게시글을 찾을 수 없습니다");
        
        verify(postRepository, times(1)).findById(postId);
    }

    @Test
    @DisplayName("모든 게시글 조회 테스트")
    void getAllPosts() {
        // given
        Post post1 = Post.builder()
                .id(1L)
                .title("테스트 제목 1")
                .content("테스트 내용 1")
                .writer("테스트 작성자 1")
                .user(testUser)
                .category(defaultCategory)
                .build();

        Post post2 = Post.builder()
                .id(2L)
                .title("테스트 제목 2")
                .content("테스트 내용 2")
                .writer("테스트 작성자 2")
                .user(testUser)
                .category(defaultCategory)
                .build();

        List<Post> posts = Arrays.asList(post1, post2);

        given(postRepository.findAllByOrderByCreatedAtDesc()).willReturn(posts);

        // when
        List<PostResponseDto> responseDtos = postService.getAllPosts();

        // then
        assertThat(responseDtos).isNotNull();
        assertThat(responseDtos).hasSize(2);
        assertThat(responseDtos.get(0).getId()).isEqualTo(post1.getId());
        assertThat(responseDtos.get(0).getTitle()).isEqualTo(post1.getTitle());
        assertThat(responseDtos.get(1).getId()).isEqualTo(post2.getId());
        assertThat(responseDtos.get(1).getTitle()).isEqualTo(post2.getTitle());
        
        verify(postRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("카테고리별 게시글 조회 테스트")
    void getPostsByCategory() {
        // given
        Long categoryId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .name("테스트 카테고리")
                .build();

        CategoryResponseDto categoryResponseDto = CategoryResponseDto.builder()
                .id(categoryId)
                .name("테스트 카테고리")
                .build();

        Post post1 = Post.builder()
                .id(1L)
                .title("카테고리 테스트 1")
                .content("카테고리 내용 1")
                .writer("작성자1")
                .category(category)
                .build();

        given(categoryService.getCategory(categoryId)).willReturn(categoryResponseDto);
        given(postRepository.findByCategoryOrderByCreatedAtDesc(any(Category.class)))
                .willReturn(Arrays.asList(post1));

        // when
        List<PostResponseDto> result = postService.getPostsByCategory(categoryId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("카테고리 테스트 1");
        
        verify(categoryService, times(1)).getCategory(categoryId);
        verify(postRepository, times(1)).findByCategoryOrderByCreatedAtDesc(any(Category.class));
    }

    @Test
    @DisplayName("게시글 수정 성공 테스트 (파일 없음)")
    void updatePostWithoutFile() {
        // given
        Long postId = 1L;
        Post existingPost = Post.builder()
                .id(postId)
                .title("원본 제목")
                .content("원본 내용")
                .writer("원본 작성자")
                .user(testUser)
                .category(defaultCategory)
                .build();

        PostRequestDto requestDto = PostRequestDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .writer("원본 작성자")
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));

        // when
        PostResponseDto responseDto = postService.updatePost(postId, requestDto, testUser);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(postId);
        assertThat(responseDto.getTitle()).isEqualTo(requestDto.getTitle());
        assertThat(responseDto.getContent()).isEqualTo(requestDto.getContent());
        assertThat(responseDto.getWriter()).isEqualTo(requestDto.getWriter());
        
        verify(postRepository, times(1)).findById(postId);
        verify(s3Uploader, never()).upload(any(MultipartFile.class), anyString());
        verify(s3Uploader, never()).delete(anyString());
    }

    @Test
    @DisplayName("게시글 수정 성공 테스트 (파일 포함, 기존 파일 교체)")
    void updatePostWithFile() {
        // given
        Long postId = 1L;
        String originalFileKey = "posts/original.jpg";
        
        Post existingPost = Post.builder()
                .id(postId)
                .title("원본 제목")
                .content("원본 내용")
                .writer("원본 작성자")
                .user(testUser)
                .fileUrl("https://example.com/posts/original.jpg")
                .fileName("original.jpg")
                .fileKey(originalFileKey)
                .category(defaultCategory)
                .build();

        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "updated.jpg",
                "image/jpeg",
                "updated image content".getBytes()
        );

        PostRequestDto requestDto = PostRequestDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .writer("원본 작성자")
                .file(newFile)
                .build();

        String newFileUrl = "https://example.com/posts/updated.jpg";
        given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));
        given(s3Uploader.upload(any(MultipartFile.class), eq("posts"))).willReturn(newFileUrl);
        doNothing().when(s3Uploader).delete(anyString());

        // when
        PostResponseDto responseDto = postService.updatePost(postId, requestDto, testUser);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(postId);
        assertThat(responseDto.getTitle()).isEqualTo(requestDto.getTitle());
        assertThat(responseDto.getContent()).isEqualTo(requestDto.getContent());
        assertThat(responseDto.getFileUrl()).isEqualTo(newFileUrl);
        assertThat(responseDto.getFileName()).isEqualTo(newFile.getOriginalFilename());
        
        verify(postRepository, times(1)).findById(postId);
        verify(s3Uploader, times(1)).delete(originalFileKey);
        verify(s3Uploader, times(1)).upload(any(MultipartFile.class), eq("posts"));
    }

    @Test
    @DisplayName("게시글 삭제 성공 테스트 (파일 포함)")
    void deletePostWithFile() {
        // given
        Long postId = 1L;
        String fileKey = "posts/test.jpg";
        
        Post post = Post.builder()
                .id(postId)
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .user(testUser)
                .fileUrl("https://example.com/posts/test.jpg")
                .fileName("test.jpg")
                .fileKey(fileKey)
                .category(defaultCategory)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        doNothing().when(s3Uploader).delete(anyString());
        doNothing().when(postRepository).delete(any(Post.class));

        // when
        postService.deletePost(postId);

        // then
        verify(postRepository, times(1)).findById(postId);
        verify(s3Uploader, times(1)).delete(fileKey);
        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("게시글 삭제 성공 테스트 (파일 없음)")
    void deletePostWithoutFile() {
        // given
        Long postId = 1L;
        
        Post post = Post.builder()
                .id(postId)
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .user(testUser)
                .category(defaultCategory)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        doNothing().when(postRepository).delete(any(Post.class));

        // when
        postService.deletePost(postId);

        // then
        verify(postRepository, times(1)).findById(postId);
        verify(s3Uploader, never()).delete(anyString());
        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("게시글 삭제 실패 테스트 - 존재하지 않는 게시글")
    void deletePostNotFound() {
        // given
        Long postId = 99L;
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(postId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("해당 ID의 게시글을 찾을 수 없습니다");
        
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("Writer가 null일 때 User의 username 사용 테스트")
    void createPostWithNullWriterUsesUsername() {
        // given
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                // writer는 null
                .build();

        Post savedPost = Post.builder()
                .id(1L)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .writer(testUser.getUsername()) // User의 username 사용
                .user(testUser)
                .category(defaultCategory)
                .build();

        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        PostResponseDto responseDto = postService.createPost(requestDto, testUser);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getWriter()).isEqualTo(testUser.getUsername());
        
        verify(postRepository, times(1)).save(any(Post.class));
    }
}
