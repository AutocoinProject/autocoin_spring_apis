package com.autocoin.integration;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestEntityConfig;
import com.autocoin.config.TestSchedulingConfig;
import com.autocoin.config.TestWebConfig;
import com.autocoin.config.TestJwtConfig;
import com.autocoin.file.application.service.S3UploaderInterface;
import com.autocoin.post.domain.entity.Post;
import com.autocoin.post.dto.response.PostResponseDto;
import com.autocoin.post.infrastructure.repository.PostJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 CRUD 통합 테스트
 * 실제 데이터베이스를 사용한 게시글의 전체 생명주기 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestEntityConfig.class, TestSchedulingConfig.class, TestWebConfig.class, TestJwtConfig.class})
@Transactional
@WithMockUser(username = "test@example.com", roles = "USER")
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @MockBean
    private S3UploaderInterface s3Uploader;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트 데이터 초기화
        postJpaRepository.deleteAll();

        // S3 업로더 Mock 설정
        given(s3Uploader.upload(any(), anyString())).willReturn("https://example.com/test.jpg");
        doNothing().when(s3Uploader).delete(anyString());
    }

    @Test
    @DisplayName("게시글 CRUD 전체 플로우 통합 테스트")
    void postCRUDIntegrationTest() throws Exception {
        // 1. 게시글 생성
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String title = "통합 테스트 제목";
        String content = "통합 테스트 내용";
        String writer = "통합 테스트 작성자";

        String createResult = mockMvc.perform(multipart("/api/v1/posts")
                        .file(file)
                        .param("title", title)
                        .param("content", content)
                        .param("writer", writer)
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.writer").value(writer))
                .andExpect(jsonPath("$.fileUrl").value("https://example.com/test.jpg"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 생성된 게시글 ID 추출
        PostResponseDto createdPost = objectMapper.readValue(createResult, PostResponseDto.class);
        Long postId = createdPost.getId();

        // 2. 생성된 게시글 조회
        mockMvc.perform(get("/api/v1/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.writer").value(writer));

        // 3. 게시글 수정
        String updatedTitle = "수정된 제목";
        String updatedContent = "수정된 내용";

        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "updated.jpg",
                "image/jpeg",
                "updated image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/posts/{id}", postId)
                        .file(updatedFile)
                        .param("title", updatedTitle)
                        .param("content", updatedContent)
                        .param("writer", writer)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.writer").value(writer));

        // 4. 게시글 삭제
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 5. 삭제 확인
        assertThat(postJpaRepository.findById(postId)).isEmpty();
    }

    @Test
    @DisplayName("파일 없이 게시글 작성 테스트")
    void createPostWithoutFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "application/octet-stream",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/posts")
                        .file(emptyFile)
                        .param("title", "파일 없는 게시글")
                        .param("content", "파일 없이 작성된 게시글입니다")
                        .param("writer", "testuser")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("파일 없는 게시글"))
                .andExpect(jsonPath("$.content").value("파일 없이 작성된 게시글입니다"))
                .andExpect(jsonPath("$.writer").value("testuser"));
    }

    @Test
    @DisplayName("전체 게시글 목록 조회 테스트")
    void getAllPosts() throws Exception {
        // 게시글 여러 개 생성
        Post post1 = postJpaRepository.save(Post.builder()
                .title("제목 1")
                .content("내용 1")
                .writer("작성자 1")
                .build());

        Post post2 = postJpaRepository.save(Post.builder()
                .title("제목 2")
                .content("내용 2")
                .writer("작성자 2")
                .build());

        // 전체 게시글 조회
        mockMvc.perform(get("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[1].title").exists());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 404 에러 테스트")
    void getPostNotFound() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(get("/api/v1/posts/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시 404 에러 테스트")
    void updatePostNotFound() throws Exception {
        Long nonExistentId = 99999L;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/posts/{id}", nonExistentId)
                        .file(file)
                        .param("title", "수정 시도")
                        .param("content", "존재하지 않는 게시글 수정 시도")
                        .param("writer", "testuser")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시 404 에러 테스트")
    void deletePostNotFound() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(delete("/api/v1/posts/{id}", nonExistentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 작성 시 필수 필드 누락 검증 테스트")
    void createPostWithMissingFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // 제목 누락
        mockMvc.perform(multipart("/api/v1/posts")
                        .file(file)
                        .param("content", "내용만 있는 게시글")
                        .param("writer", "testuser")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 내용 누락
        mockMvc.perform(multipart("/api/v1/posts")
                        .file(file)
                        .param("title", "제목만 있는 게시글")
                        .param("writer", "testuser")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 작성자 누락
        mockMvc.perform(multipart("/api/v1/posts")
                        .file(file)
                        .param("title", "작성자 없는 게시글")
                        .param("content", "작성자가 없는 내용")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
