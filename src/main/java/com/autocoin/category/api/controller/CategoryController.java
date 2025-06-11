package com.autocoin.category.api.controller;

import com.autocoin.category.application.service.CategoryService;
import com.autocoin.category.dto.request.CategoryRequestDto;
import com.autocoin.category.dto.response.CategoryResponseDto;
import com.autocoin.user.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "카테고리 API", description = "카테고리 CRUD API")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 카테고리 초기화 API
     * @return 성공 메시지
     */
    @Operation(summary = "카테고리 초기화", description = "시스템 기본 카테고리를 초기화합니다. 관리자 권한 필요.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "초기화 성공", content = @Content),
        @ApiResponse(responseCode = "401", description = "테스트 성공", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping("/init")
    @Secured("ADMIN")
    public ResponseEntity<String> initCategories() {
        categoryService.initDefaultCategories();
        return ResponseEntity.ok("카테고리 초기화가 완료되었습니다.");
    }

    /**
     * 카테고리 목록 조회 API (계층 구조)
     * @return 계층 구조의 카테고리 목록
     */
    @Operation(summary = "카테고리 목록 조회 (계층 구조)", description = "모든 카테고리를 계층 구조로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
    
    /**
     * 카테고리 목록 조회 API (평면 구조)
     * @return 평면 구조의 카테고리 목록
     */
    @Operation(summary = "카테고리 목록 조회 (평면 구조)", description = "모든 카테고리를 평면 구조로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/flat")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategoriesFlat() {
        return ResponseEntity.ok(categoryService.getAllCategoriesFlat());
    }

    /**
     * 카테고리 상세 조회 API
     * @param id 카테고리 ID
     * @return 카테고리 상세 정보
     */
    @Operation(summary = "카테고리 상세 조회", description = "ID를 사용하여 특정 카테고리의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategory(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    /**
     * 카테고리 생성 API
     * @param requestDto 카테고리 생성 요청 DTO
     * @return 생성된 카테고리 정보
     */
    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다. 관리자 권한 필요.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "카테고리 생성 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 요청", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping
    @Secured("ADMIN")
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(requestDto));
    }

    /**
     * 카테고리 수정 API
     * @param id 카테고리 ID
     * @param requestDto 카테고리 수정 요청 DTO
     * @return 수정된 카테고리 정보
     */
    @Operation(summary = "카테고리 수정", description = "특정 카테고리의 정보를 수정합니다. 관리자 권한 필요.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 요청", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PutMapping("/{id}")
    @Secured("ADMIN")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto requestDto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, requestDto));
    }

    /**
     * 카테고리 삭제 API
     * @param id 카테고리 ID
     * @return 응답 없음 (204 No Content)
     */
    @Operation(summary = "카테고리 삭제", description = "특정 카테고리를 삭제합니다. 관리자 권한 필요.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @DeleteMapping("/{id}")
    @Secured("ADMIN")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
