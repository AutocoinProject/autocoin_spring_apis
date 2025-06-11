package com.autocoin.integration;

import com.autocoin.config.TestConfig;
import com.autocoin.config.TestEntityConfig;
import com.autocoin.config.TestSchedulingConfig;
import com.autocoin.config.TestWebConfig;
import com.autocoin.config.TestJwtConfig;
import com.autocoin.category.domain.CategoryRepository;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.category.dto.request.CategoryRequestDto;
import com.autocoin.post.infrastructure.repository.PostJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 카테고리와 게시글 통합 테스트
 * 카테고리 생성부터 해당 카테고리의 게시글 작성까지의 전체 플로우를 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestEntityConfig.class, TestSchedulingConfig.class, TestWebConfig.class, TestJwtConfig.class})
@Transactional
@TestMethodOrder(OrderAnnotation.class)
class CategoryPostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostJpaRepository postJpaRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 데이터 초기화 - JpaRepository의 deleteAll() 사용
        postJpaRepository.deleteAll();
        clearCategoryData();

        // 테스트용 카테고리 생성
        testCategory = categoryRepository.save(Category.builder()
                .name("테스트 카테고리")
                .description("테스트용 카테고리입니다")
                .build());
    }
    
    /**
     * 카테고리 데이터 정리 (도메인 Repository는 deleteAll이 없으므로 직접 구현)
     * 현재는 테스트별로 유니크한 데이터 사용으로 대체
     */
    private void clearCategoryData() {
        // 실제 구현에서는 CategoryRepository에 findAll과 delete가 있어야 함
        // 또는 @Sql 어노테이션 사용하여 데이터 정리
        // 여기서는 간단히 생략하고 테스트별로 유니크한 데이터 사용
        // 현재는 비어있지만 추후 필요시 구현 가능
    }

    @Test
    @Order(1)
    @DisplayName("카테고리를 지정하여 게시글 작성 후 카테고리별 조회 테스트")
    @WithMockUser(roles = "USER")
    void createPostWithCategoryAndGetByCategory() throws Exception {
        // 1. 카테고리가 지정된 게시글 생성
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                        .file(file)
                        .param("title", "카테고리 테스트 게시글")
                        .param("content", "카테고리가 지정된 게시글입니다")
                        .param("writer", "testuser1")
                        .param("categoryId", testCategory.getId().toString())
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("카테고리 테스트 게시글"))
                .andExpect(jsonPath("$.categoryId").value(testCategory.getId()));

        // 2. 카테고리별 게시글 목록 조회
        mockMvc.perform(get("/api/v1/posts/category/" + testCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("카테고리 테스트 게시글"))
                .andExpect(jsonPath("$[0].categoryId").value(testCategory.getId()));
    }

    @Test
    @Order(2)
    @DisplayName("카테고리 생성 및 해당 카테고리 게시글 작성 통합 테스트")
    @WithMockUser(roles = "ADMIN")
    void createCategoryAndCreatePostIntegration() throws Exception {
        // 1. 새 카테고리 생성
        CategoryRequestDto categoryRequestDto = CategoryRequestDto.builder()
                .name("새 카테고리")
                .description("새로운 카테고리입니다")
                .build();

        String categoryResponse = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequestDto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("새 카테고리"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 생성된 카테고리 ID 추출 - JsonNode 사용으로 안전한 파싱
        JsonNode jsonNode = objectMapper.readTree(categoryResponse);
        Long categoryId = jsonNode.get("id").asLong();

        // 2. 생성된 카테고리에 게시글 작성
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                        .file(file)
                        .param("title", "새 카테고리 게시글")
                        .param("content", "새 카테고리에 작성된 게시글입니다")
                        .param("writer", "testuser2")
                        .param("categoryId", String.valueOf(categoryId))
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("새 카테고리 게시글"))
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        // 3. 카테고리별 게시글 목록 조회
        mockMvc.perform(get("/api/v1/posts/category/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("새 카테고리 게시글"))
                .andExpect(jsonPath("$[0].categoryId").value(categoryId));
    }

    @Test
    @Order(3)
    @DisplayName("여러 카테고리에 게시글 작성 후 카테고리별 분류 확인 테스트")
    @WithMockUser(roles = "USER")
    void multiCategoryPostTest() throws Exception {
        // 추가 카테고리 생성
        Category category2 = categoryRepository.save(Category.builder()
                .name("두 번째 카테고리")
                .description("두 번째 테스트 카테고리")
                .build());

        // 첫 번째 카테고리에 게시글 2개 작성
        for (int i = 1; i <= 2; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test" + i + ".jpg",
                    "image/jpeg",
                    ("test image content " + i).getBytes()
            );

            mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                            .file(file)
                            .param("title", "첫 번째 카테고리 게시글 " + i)
                            .param("content", "첫 번째 카테고리 내용 " + i)
                            .param("writer", "testuser" + i)
                            .param("categoryId", testCategory.getId().toString())
                            .with(csrf())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated());
        }

        // 두 번째 카테고리에 게시글 1개 작성
        MockMultipartFile file3 = new MockMultipartFile(
                "file",
                "test3.jpg",
                "image/jpeg",
                "test image content 3".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                        .file(file3)
                        .param("title", "두 번째 카테고리 게시글")
                        .param("content", "두 번째 카테고리 내용")
                        .param("writer", "testuser3")
                        .param("categoryId", category2.getId().toString())
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        // 첫 번째 카테고리 게시글 조회 (2개 확인)
        mockMvc.perform(get("/api/v1/posts/category/" + testCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 두 번째 카테고리 게시글 조회 (1개 확인)
        mockMvc.perform(get("/api/v1/posts/category/" + category2.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("두 번째 카테고리 게시글"));
    }

    @Test
    @Order(4)
    @DisplayName("존재하지 않는 카테고리로 게시글 작성 시 실패 테스트")
    @WithMockUser(roles = "USER")
    void createPostWithNonExistentCategory() throws Exception {
        Long nonExistentCategoryId = 99999L;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                        .file(file)
                        .param("title", "존재하지 않는 카테고리 게시글")
                        .param("content", "존재하지 않는 카테고리로 작성 시도")
                        .param("writer", "testuser4")
                        .param("categoryId", nonExistentCategoryId.toString())
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("카테고리 없이 게시글 작성 테스트 (기본 카테고리 사용)")
    @WithMockUser(roles = "USER")
    void createPostWithoutCategory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                        .file(file)
                        .param("title", "카테고리 없는 게시글")
                        .param("content", "카테고리를 지정하지 않은 게시글")
                        .param("writer", "testuser5")
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("카테고리 없는 게시글"));
    }

    @Test
    @Order(6)
    @DisplayName("인증 없이 게시글 작성 시도 테스트")
    void createPostWithoutAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/posts")
                        .file(file)
                        .param("title", "인증 없는 게시글")
                        .param("content", "인증 없이 작성 시도")
                        .param("writer", "anonymous")
                        .param("categoryId", testCategory.getId().toString())
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
