package com.autocoin.category.api;

import com.autocoin.category.api.controller.CategoryController;
import com.autocoin.category.application.service.CategoryService;
import com.autocoin.category.dto.request.CategoryRequestDto;
import com.autocoin.category.dto.response.CategoryResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 목록 조회 테스트")
    public void testGetAllCategories() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<CategoryResponseDto> categories = Arrays.asList(
                CategoryResponseDto.builder()
                        .id(1L)
                        .name("공지사항")
                        .description("관리자 공지사항입니다")
                        .children(Collections.emptyList())
                        .createdAt(now)
                        .updatedAt(now)
                        .build(),
                CategoryResponseDto.builder()
                        .id(2L)
                        .name("자유게시판")
                        .description("자유롭게 대화를 나누는 공간입니다")
                        .children(Collections.emptyList())
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        when(categoryService.getAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("공지사항"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("자유게시판"));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("카테고리 상세 조회 테스트")
    public void testGetCategory() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        CategoryResponseDto category = CategoryResponseDto.builder()
                .id(1L)
                .name("공지사항")
                .description("관리자 공지사항입니다")
                .children(Collections.emptyList())
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(categoryService.getCategory(anyLong())).thenReturn(category);

        // When & Then
        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("공지사항"))
                .andExpect(jsonPath("$.description").value("관리자 공지사항입니다"));

        verify(categoryService, times(1)).getCategory(1L);
    }

    @Test
    @DisplayName("카테고리 생성 테스트 (관리자 권한)")
    @WithMockUser(roles = "ADMIN")
    public void testCreateCategory() throws Exception {
        // Given
        CategoryRequestDto requestDto = CategoryRequestDto.builder()
                .name("새 카테고리")
                .description("새로운 카테고리입니다")
                .build();

        LocalDateTime now = LocalDateTime.now();
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .id(3L)
                .name("새 카테고리")
                .description("새로운 카테고리입니다")
                .children(Collections.emptyList())
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(categoryService.createCategory(any(CategoryRequestDto.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("새 카테고리"))
                .andExpect(jsonPath("$.description").value("새로운 카테고리입니다"));

        verify(categoryService, times(1)).createCategory(any(CategoryRequestDto.class));
    }
}
