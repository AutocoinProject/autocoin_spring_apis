package com.autocoin.post.application.service;

import com.autocoin.category.application.service.CategoryService;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.file.application.service.S3UploaderInterface;
import com.autocoin.global.exception.business.ResourceNotFoundException;
import com.autocoin.post.domain.entity.Post;
import com.autocoin.post.dto.request.PostRequestDto;
import com.autocoin.post.dto.response.PostResponseDto;
import com.autocoin.post.domain.PostRepository;
import com.autocoin.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final S3UploaderInterface s3Uploader;
    private final CategoryService categoryService;
    private static final String S3_DIRECTORY = "posts";

    /**
     * 게시글 작성
     * @param requestDto 게시글 작성 요청 DTO
     * @param user 작성자
     * @return 작성된 게시글 응답 DTO
     */
    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto, User user) {
        try {
            // 사용자 정보 처리 - 사용자가 null이면 로그 추가 및 디버깅
            if (user == null) {
                log.warn("사용자 정보 없이 게시글 작성 요청 - 익명 게시글로 처리");
            } else {
                log.info("인증 사용자({}, {}, {})가 게시글 작성", user.getId(), user.getUsername(), user.getEmail());
            }
            
            // 파일 업로드 처리 (S3 연결 실패해도 게시글 작성 가능)
            String fileUrl = null;
            String fileName = null;
            String fileKey = null;
            
            MultipartFile file = requestDto.getFile();
            if (file != null && !file.isEmpty()) {
                log.info("파일 업로드 시작: 파일명={}, 사이즈={} bytes", 
                        file.getOriginalFilename(), file.getSize());
                
                try {
                    // S3 업로드 시도
                    fileUrl = s3Uploader.upload(file, S3_DIRECTORY);
                    fileName = file.getOriginalFilename();
                    
                    if (fileUrl != null) {
                        // S3 URL에서 fileKey 추출
                        String[] urlParts = fileUrl.split("/");
                        if (urlParts.length >= 2) {
                            fileKey = urlParts[urlParts.length - 2] + "/" + urlParts[urlParts.length - 1];
                        } else {
                            fileKey = fileUrl.substring(fileUrl.indexOf("/", 8) + 1);
                        }
                        log.info("파일 업로드 성공: URL={}, fileKey={}", fileUrl, fileKey);
                    } else {
                        log.warn("S3 업로드 실패 - 파일 없이 게시글 작성 진행");
                        fileName = null; // 업로드 실패시 파일명도 제거
                    }
                } catch (Exception e) {
                    log.error("파일 업로드 중 오류 발생, 파일 없이 게시글 작성 진행: {}", e.getMessage());
                    fileUrl = null;
                    fileName = null;
                    fileKey = null;
                }
            }

            // 작성자 정보 처리 - writer가 없으면 현재 사용자의 username 사용
            String writerName;
            if (requestDto.getWriter() != null && !requestDto.getWriter().isEmpty()) {
                writerName = requestDto.getWriter();
            } else if (user != null) {
                writerName = user.getUsername();
            } else {
                writerName = "anonymous";
            }
            
            // 제목과 내용이 없는 경우 기본값 설정
            String title = (requestDto.getTitle() != null && !requestDto.getTitle().isEmpty())
                    ? requestDto.getTitle()
                    : "제목 없음";
                    
            String content = (requestDto.getContent() != null && !requestDto.getContent().isEmpty())
                    ? requestDto.getContent()
                    : "내용 없음";
            
            // 카테고리 처리
            Category category;
            try {
                if (requestDto.getCategoryId() != null) {
                    try {
                        category = categoryService.getCategory(requestDto.getCategoryId()).toEntity();
                    } catch (ResourceNotFoundException e) {
                        log.warn("요청한 카테고리 ID {} 가 존재하지 않으므로 기본 카테고리를 사용합니다.", requestDto.getCategoryId());
                        category = categoryService.getDefaultCategory();
                    }
                } else {
                    category = categoryService.getDefaultCategory();
                }
            } catch (Exception e) {
                // 카테고리 처리 중 오류 발생 시
                log.error("카테고리 처리 중 오류 발생", e);
                category = null; // 카테고리 없이 진행
            }

            // 게시글 저장
            Post.PostBuilder postBuilder = Post.builder()
                    .title(title)
                    .content(content)
                    .writer(writerName)
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .fileKey(fileKey);
            
            // 사용자가 있는 경우에만 user 설정
            if (user != null) {
                postBuilder.user(user);
            }
            
            // 카테고리가 있는 경우에만 category 설정
            if (category != null) {
                postBuilder.category(category);
            }
            
            Post post = postBuilder.build();

            Post savedPost = postRepository.save(post);
            log.info("게시글 저장 완료 - ID: {}, 제목: {}, 작성자: {}", savedPost.getId(), savedPost.getTitle(), savedPost.getWriter());
            return PostResponseDto.of(savedPost);
        } catch (Exception e) {
            log.error("게시글 작성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("게시글 작성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 게시글 상세 조회
     * @param id 게시글 ID
     * @return 게시글 응답 DTO
     */
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 게시글을 찾을 수 없습니다: " + id));
        return PostResponseDto.of(post);
    }

    /**
     * 게시글 목록 조회
     * @return 게시글 목록
     */
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PostResponseDto::of)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자별 게시글 목록 조회
     * @param user 사용자 객체
     * @return 해당 사용자의 게시글 목록
     */
    public List<PostResponseDto> getPostsByUser(User user) {
        return postRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(PostResponseDto::of)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 게시글 목록 조회 (기본 페이징 없음)
     * @param categoryId 카테고리 ID
     * @return 게시글 목록
     */
    public List<PostResponseDto> getPostsByCategory(Long categoryId) {
        Category category = categoryService.getCategory(categoryId).toEntity();
        return postRepository.findByCategoryOrderByCreatedAtDesc(category).stream()
                .map(PostResponseDto::of)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 게시글 목록 조회 (페이징 적용)
     * @param categoryId 카테고리 ID
     * @param pageable 페이징 정보
     * @return 페이징된 게시글 목록
     */
    public Page<PostResponseDto> getPostsByCategoryPaged(Long categoryId, Pageable pageable) {
        Category category = categoryService.getCategory(categoryId).toEntity();
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable)
                .map(PostResponseDto::of);
    }

    /**
     * 게시글 수정
     * @param id 게시글 ID
     * @param requestDto 게시글 수정 요청 DTO
     * @param user 수정자
     * @return 수정된 게시글 응답 DTO
     */
    @Transactional
    public PostResponseDto updatePost(Long id, PostRequestDto requestDto, User user) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 게시글을 찾을 수 없습니다: " + id));

        // 사용자 권한 검증 추가
        if (user == null) {
            log.warn("인증되지 않은 사용자가 게시글 수정 시도 - ID: {}", id);
        } else if (post.getUser() != null && !post.getUser().getId().equals(user.getId())) {
            log.warn("다른 사용자의 게시글 수정 시도 - 게시글 ID: {}, 작성자 ID: {}, 요청자 ID: {}", 
                    id, post.getUser().getId(), user.getId());
        }

        try {
            // 새 파일이 업로드된 경우
            String fileUrl = null;
            String fileName = null;
            String fileKey = null;

            MultipartFile file = requestDto.getFile();
            if (file != null && !file.isEmpty()) {
                log.info("S3 파일 업로드 시작 (수정): 파일명={}, 사이즈={} bytes", 
                        file.getOriginalFilename(), file.getSize());
                
                // 기존 파일이 있으면 삭제
                if (post.getFileKey() != null) {
                    log.info("S3에서 기존 파일 삭제: {}", post.getFileKey());
                    s3Uploader.delete(post.getFileKey());
                }
                
                // 새 파일 업로드
                fileUrl = s3Uploader.upload(file, S3_DIRECTORY);
                fileName = file.getOriginalFilename();
                
                // S3 URL에서 올바른 fileKey 추출
                if (fileUrl != null) {
                    String[] urlParts = fileUrl.split("/");
                    if (urlParts.length >= 2) {
                        fileKey = urlParts[urlParts.length - 2] + "/" + urlParts[urlParts.length - 1];
                    } else {
                        fileKey = fileUrl.substring(fileUrl.indexOf("/", 8) + 1);
                    }
                    log.info("S3 파일 업로드 성공 (수정): URL={}, fileKey={}", fileUrl, fileKey);
                }
            }
            
            // 작성자 정보 처리 - writer가 없으면 현재 사용자의 username 사용 또는 기존 writer 유지
            String writerName = post.getWriter(); // 기본적으로 기존 작성자 정보 유지
            if (requestDto.getWriter() != null && !requestDto.getWriter().isEmpty()) {
                writerName = requestDto.getWriter(); // 새 작성자 정보가 있으면 사용
            }
            
            // 제목과 내용 처리 - null 또는 빈 문자열이면 기존 값 유지
            String title = (requestDto.getTitle() != null && !requestDto.getTitle().isEmpty())
                    ? requestDto.getTitle()
                    : post.getTitle();
            
            String content = (requestDto.getContent() != null && !requestDto.getContent().isEmpty())
                    ? requestDto.getContent()
                    : post.getContent();
                    
            // 카테고리 처리
            Category category = post.getCategory(); // 기본적으로 기존 카테고리 유지
            if (requestDto.getCategoryId() != null) {
                try {
                    category = categoryService.getCategory(requestDto.getCategoryId()).toEntity();
                    log.info("게시글 {} 카테고리 변경: {} -> {}", 
                            id, 
                            post.getCategory() != null ? post.getCategory().getId() : "null", 
                            requestDto.getCategoryId());
                } catch (ResourceNotFoundException e) {
                    log.warn("요청한 카테고리 ID {}(가) 존재하지 않으므로 기존 카테고리를 유지합니다.", requestDto.getCategoryId());
                }
            }

            // 게시글 업데이트
            post.update(title, content, fileUrl, fileName, fileKey, writerName, category);
            log.info("게시글 수정 완료 - ID: {}, 제목: {}", post.getId(), post.getTitle());
            
            return PostResponseDto.of(post);
        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("게시글 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 게시글 삭제
     * @param id 게시글 ID
     */
    @Transactional
    public void deletePost(Long id) {
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 게시글을 찾을 수 없습니다: " + id));

            log.info("게시글 삭제 시작 - ID: {}, 제목: {}, 작성자: {}", 
                    post.getId(), post.getTitle(), post.getWriter());

            // S3에서 파일 삭제
            if (post.getFileKey() != null) {
                try {
                    s3Uploader.delete(post.getFileKey());
                    log.info("S3 파일 삭제 성공 - 파일키: {}", post.getFileKey());
                } catch (Exception e) {
                    // 파일 삭제 실패를 로그로 기록하지만 게시글 삭제는 계속 진행
                    log.warn("S3 파일 삭제 오류 - 파일키: {}, 오류: {}", post.getFileKey(), e.getMessage());
                }
            }

            // 게시글 삭제
            postRepository.delete(post);
            log.info("게시글 삭제 완료 - ID: {}", id);
        } catch (ResourceNotFoundException e) {
            // 이미 삭제된 게시글일 경우
            log.warn("존재하지 않는 게시글 삭제 시도 - ID: {}", id);
            throw e; // 원래 예외 그대로 다시 넘김
        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생 - ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new RuntimeException("게시글 삭제 중 오류가 발생했습니다.", e);
        }
    }
}
