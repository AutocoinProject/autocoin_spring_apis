package com.autocoin.post.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.autocoin.post.application.service.PostService;
import com.autocoin.post.dto.request.PostRequestDto;
import com.autocoin.post.dto.response.PostResponseDto;
import com.autocoin.user.domain.User;
import com.autocoin.global.auth.provider.JwtTokenProvider;
import com.autocoin.user.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "게시글 API", description = "게시글 등록, 수정, 조회, 삭제 API")
public class PostController {

    private final PostService postService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * 게시글 작성 API
     */
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다. 파일 업로드도 가능합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "게시글 작성 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 요청", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto> createPost(
            @Parameter(description = "제목") @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "내용") @RequestParam(value = "content", required = false) String content,
            @Parameter(description = "작성자") @RequestParam(value = "writer", required = false) String writer,
            @Parameter(description = "카테고리 ID") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "업로드할 파일") @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        
        // 기본값 설정
        String finalTitle = (title != null && !title.isEmpty()) ? title : "제목 없음";
        String finalContent = (content != null && !content.isEmpty()) ? content : "내용 없음";
        String finalWriter = (writer != null && !writer.isEmpty()) ? writer : (user != null ? user.getUsername() : "anonymous");
        
        // RequestDto 생성
        PostRequestDto requestDto = PostRequestDto.builder()
                .title(finalTitle)
                .content(finalContent)
                .writer(finalWriter)
                .categoryId(categoryId)
                .file(file)
                .build();
        
        log.debug("Post request received:\n" +
                "  - Title: {}\n" +
                "  - Content: {}\n" +
                "  - Writer: {}\n" +
                "  - File: {}\n" +
                "  - User: {}", 
                finalTitle, 
                finalContent, 
                finalWriter, 
                file != null ? file.getOriginalFilename() : "null",
                user != null ? "id=" + user.getId() + ", username=" + user.getUsername() : "null");
        
        // 인증 정보가 없는 경우 처리
        if (user == null) {
            log.warn("인증되지 않은 사용자의 게시글 작성 시도");
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "게시글 작성을 위해 로그인이 필요합니다.");
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(requestDto, user));
    }
    

    
    /**
     * 테스트용 게시글 작성 API - 유효성 검증 없음
     */
    @Operation(summary = "(테스트용) 게시글 작성", description = "유효성 검증 없이 게시글을 작성합니다. 테스트용입니다.")
    @PostMapping(value = "/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto> createTestPost(
            @Parameter(description = "제목") @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "내용") @RequestParam(value = "content", required = false) String content,
            @Parameter(description = "작성자") @RequestParam(value = "writer", required = false) String writer,
            @Parameter(description = "카테고리 ID") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "업로드할 파일") @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) {
        
        log.debug("Test post request received:\n" +
                "  - Title: {}\n" +
                "  - Content: {}\n" +
                "  - Writer: {}\n" +
                "  - File: {}\n" +
                "  - CategoryId: {}", 
                title, content, writer, 
                file != null ? file.getOriginalFilename() : "null",
                categoryId);
        
        try {
            // 기본값 설정
            String finalTitle = (title != null && !title.isEmpty()) ? title : "테스트 제목";
            String finalContent = (content != null && !content.isEmpty()) ? content : "테스트 내용";
            String finalWriter = (writer != null && !writer.isEmpty()) ? writer : "anonymous";
            
            // DTO 생성
            PostRequestDto requestDto = PostRequestDto.builder()
                    .title(finalTitle)
                    .content(finalContent)
                    .writer(finalWriter)
                    .categoryId(categoryId)
                    .file(file)
                    .build();
            
            // 기본 처리 로깅
            log.info("익명 게시글 작성 요청 - 제목: {}, 카테고리 ID: {}", finalTitle, categoryId);
            
            // 사용자 정보는 null로 전달 (선택적 관계)
            PostResponseDto result = postService.createPost(requestDto, null);
            log.info("익명 게시글 작성 성공 - ID: {}", result.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("익명 게시글 작성 오류: {}", e.getMessage(), e);
            throw e; // 예외 재전파
        }
    }

    /**
     * 게시글 목록 조회 API
     */
    @Operation(summary = "게시글 목록 조회", description = "모든 게시글을 작성일 기준 내림차순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    /**
     * 게시글 상세 조회 API
     */
    @Operation(summary = "게시글 상세 조회", description = "ID를 사용하여 특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    /**
     * 게시글 수정 API
     */
    @Operation(summary = "게시글 수정", description = "특정 게시글의 내용을 수정합니다.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto> updatePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long id,
            @Parameter(description = "제목") @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "내용") @RequestParam(value = "content", required = false) String content,
            @Parameter(description = "작성자") @RequestParam(value = "writer", required = false) String writer,
            @Parameter(description = "카테고리 ID") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "업로드할 파일") @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal User user) {
        
        // DTO 생성
        PostRequestDto requestDto = PostRequestDto.builder()
                .title(title)
                .content(content)
                .writer(writer)
                .categoryId(categoryId)
                .file(file)
                .build();
                
        return ResponseEntity.ok(postService.updatePost(id, requestDto, user));
    }

    /**
     * 게시글 삭제 API
     */
    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 사용자별 게시글 목록 조회 API
     */
    @Operation(summary = "내 게시글 목록 조회", description = "현재 인증된 사용자의 게시글을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(postService.getPostsByUser(user));
    }
    
    /**
     * 카테고리별 게시글 목록 조회 API
     */
    @Operation(summary = "카테고리별 게시글 목록 조회", description = "특정 카테고리에 속한 게시글을 조회합니다.")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<PostResponseDto>> getPostsByCategory(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long categoryId) {
        return ResponseEntity.ok(postService.getPostsByCategory(categoryId));
    }
    
    /**
     * 카테고리별 게시글 목록 조회 API (페이징)
     */
    @Operation(summary = "카테고리별 게시글 목록 조회 (페이징)", description = "특정 카테고리에 속한 게시글을 페이징하여 조회합니다.")
    @GetMapping("/category/{categoryId}/page")
    public ResponseEntity<Page<PostResponseDto>> getPostsByCategoryPaged(
            @Parameter(description = "카테고리 ID", required = true) @PathVariable Long categoryId,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPostsByCategoryPaged(categoryId, pageable));
    }
    

}
