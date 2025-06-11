package com.autocoin.post.api;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestJpaConfig;
import com.autocoin.global.exception.business.ResourceNotFoundException;
import com.autocoin.post.api.controller.PostController;
import com.autocoin.post.application.service.PostService;
import com.autocoin.post.dto.request.PostRequestDto;
import com.autocoin.post.dto.response.PostResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PostController WebMvcTest
 * 컨트롤러 계층만 테스트하며, 서비스 계층은 Mock으로 대체
 */
@WebMvcTest(controllers = PostController.class)
@Import({TestConfig.class, TestJpaConfig.class})
@ActiveProfiles("webmvc")
@WithMockUser
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @Test
    @DisplayName("게시글 생성 API 테스트")
    void createPost() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        PostResponseDto responseDto = PostResponseDto.builder()
                .id(1L)
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .fileUrl("https://example.com/test.jpg")
                .fileName("test.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // PostService의 createPost 메서드 시그니처에 맞게 설정
        given(postService.createPost(any(PostRequestDto.class), any()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(multipart("/api/v1/posts")
                        .file(file)
                        .param("title", "테스트 제목")
                        .param("content", "테스트 내용")
                        .param("writer", "테스트 작성자")
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(responseDto.getId()))
                .andExpect(jsonPath("$.title").value(responseDto.getTitle()))
                .andExpect(jsonPath("$.content").value(responseDto.getContent()))
                .andExpect(jsonPath("$.writer").value(responseDto.getWriter()))
                .andExpect(jsonPath("$.fileUrl").value(responseDto.getFileUrl()))
                .andExpect(jsonPath("$.fileName").value(responseDto.getFileName()));
    }

    @Test
    @DisplayName("전체 게시글 조회 API 테스트")
    void getAllPosts() throws Exception {
        // given
        PostResponseDto post1 = PostResponseDto.builder()
                .id(1L)
                .title("테스트 제목 1")
                .content("테스트 내용 1")
                .writer("테스트 작성자 1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PostResponseDto post2 = PostResponseDto.builder()
                .id(2L)
                .title("테스트 제목 2")
                .content("테스트 내용 2")
                .writer("테스트 작성자 2")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<PostResponseDto> posts = Arrays.asList(post1, post2);

        given(postService.getAllPosts()).willReturn(posts);

        // when & then
        mockMvc.perform(get("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(post1.getId()))
                .andExpect(jsonPath("$[0].title").value(post1.getTitle()))
                .andExpect(jsonPath("$[1].id").value(post2.getId()))
                .andExpect(jsonPath("$[1].title").value(post2.getTitle()));
    }

    @Test
    @DisplayName("특정 게시글 조회 API 테스트")
    void getPost() throws Exception {
        // given
        Long postId = 1L;
        PostResponseDto responseDto = PostResponseDto.builder()
                .id(postId)
                .title("테스트 제목")
                .content("테스트 내용")
                .writer("테스트 작성자")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(postService.getPost(postId)).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responseDto.getId()))
                .andExpect(jsonPath("$.title").value(responseDto.getTitle()))
                .andExpect(jsonPath("$.content").value(responseDto.getContent()))
                .andExpect(jsonPath("$.writer").value(responseDto.getWriter()));
    }

    @Test
    @DisplayName("게시글 수정 API 테스트")
    void updatePost() throws Exception {
        // given
        Long postId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "updated.jpg",
                "image/jpeg",
                "updated image content".getBytes()
        );

        PostResponseDto responseDto = PostResponseDto.builder()
                .id(postId)
                .title("수정된 제목")
                .content("수정된 내용")
                .writer("테스트 작성자")
                .fileUrl("https://example.com/updated.jpg")
                .fileName("updated.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(postService.updatePost(eq(postId), any(PostRequestDto.class), any()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(multipart("/api/v1/posts/{id}", postId)
                        .file(file)
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        .param("writer", "테스트 작성자")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responseDto.getId()))
                .andExpect(jsonPath("$.title").value(responseDto.getTitle()))
                .andExpect(jsonPath("$.content").value(responseDto.getContent()))
                .andExpect(jsonPath("$.writer").value(responseDto.getWriter()))
                .andExpect(jsonPath("$.fileUrl").value(responseDto.getFileUrl()))
                .andExpect(jsonPath("$.fileName").value(responseDto.getFileName()));
    }

    @Test
    @DisplayName("게시글 삭제 API 테스트")
    void deletePost() throws Exception {
        // given
        Long postId = 1L;
        doNothing().when(postService).deletePost(postId);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 404 응답")
    void getPostNotFound() throws Exception {
        // given
        Long postId = 99L;
        given(postService.getPost(postId))
                .willThrow(new ResourceNotFoundException("해당 ID의 게시글을 찾을 수 없습니다: " + postId));

        // when & then
        mockMvc.perform(get("/api/v1/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
