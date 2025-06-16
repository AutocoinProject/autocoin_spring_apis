package com.autocoin.category.application.service;

import com.autocoin.category.domain.CategoryRepository;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.category.dto.request.CategoryRequestDto;
import com.autocoin.category.dto.response.CategoryResponseDto;
import com.autocoin.global.exception.business.DuplicateResourceException;
import com.autocoin.global.exception.business.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 모든 카테고리 조회 (계층 구조)
     * @return 계층 구조의 카테고리 목록
     */
    public List<CategoryResponseDto> getAllCategories() {
        // 루트 카테고리만 조회하여 자식 카테고리는 연관 관계를 통해 반환
        return categoryRepository.findByParentIsNull().stream()
                .map(CategoryResponseDto::of)
                .collect(Collectors.toList());
    }
    
    /**
     * 모든 카테고리 조회 (평면 구조)
     * @return 평면 구조의 카테고리 목록
     */
    public List<CategoryResponseDto> getAllCategoriesFlat() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponseDto::ofWithoutChildren)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 상세 조회
     * @param id 카테고리 ID
     * @return 카테고리 상세 정보
     */
    public CategoryResponseDto getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 카테고리를 찾을 수 없습니다: " + id));
        return CategoryResponseDto.of(category);
    }

    /**
     * 카테고리 생성
     * @param requestDto 카테고리 생성 요청 DTO
     * @return 생성된 카테고리 정보
     */
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        // 중복 카테고리 이름 체크
        if (categoryRepository.existsByName(requestDto.getName())) {
            throw new DuplicateResourceException("이미 존재하는 카테고리 이름입니다: " + requestDto.getName());
        }
        
        // 부모 카테고리 설정
        Category parent = null;
        if (requestDto.getParentId() != null) {
            parent = categoryRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("상위 카테고리를 찾을 수 없습니다: " + requestDto.getParentId()));
        }
        
        // 카테고리 생성
        Category category = Category.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .parent(parent)
                .build();
        
        Category savedCategory = categoryRepository.save(category);
        
        return CategoryResponseDto.of(savedCategory);
    }

    /**
     * 카테고리 수정
     * @param id 카테고리 ID
     * @param requestDto 카테고리 수정 요청 DTO
     * @return 수정된 카테고리 정보
     */
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 카테고리를 찾을 수 없습니다: " + id));
        
        // 이름 중복 체크 (이름이 변경된 경우에만)
        if (!category.getName().equals(requestDto.getName()) && 
                categoryRepository.existsByName(requestDto.getName())) {
            throw new DuplicateResourceException("이미 존재하는 카테고리 이름입니다: " + requestDto.getName());
        }
        
        // 부모 카테고리 변경 처리
        if (requestDto.getParentId() != null && 
                (category.getParent() == null || !category.getParent().getId().equals(requestDto.getParentId()))) {
            
            // 자기 자신을 부모로 설정하는 것 방지
            if (requestDto.getParentId().equals(id)) {
                throw new IllegalArgumentException("카테고리는 자기 자신을 부모로 설정할 수 없습니다.");
            }
            
            Category parent = categoryRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("상위 카테고리를 찾을 수 없습니다: " + requestDto.getParentId()));
            
            category.setParent(parent);
        } else if (requestDto.getParentId() == null && category.getParent() != null) {
            // 부모 카테고리 제거 (루트 카테고리로 변경)
            category.setParent(null);
        }
        
        // 기본 정보 업데이트
        category.update(requestDto.getName(), requestDto.getDescription());
        
        return CategoryResponseDto.of(category);
    }

    /**
     * 카테고리 삭제
     * @param id 카테고리 ID
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 카테고리를 찾을 수 없습니다: " + id));
        
        // 자식 카테고리가 있는 경우 삭제 불가
        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("자식 카테고리가 있는 카테고리는 삭제할 수 없습니다. 먼저 자식 카테고리를 삭제하세요.");
        }
        
        categoryRepository.delete(category);
    }
    
    /**
     * 기본 카테고리 등록 (시스템 초기화 시 사용)
     */
    @Transactional
    public void initDefaultCategories() {
        // 이미 카테고리가 있으면 초기화 스킵
        if (categoryRepository.count() > 0) {
            return;
        }
        
        // 기본 카테고리 생성
        Category notice = Category.builder()
                .name("공지사항")
                .description("관리자의 공지사항입니다")
                .build();
        
        Category freeBoard = Category.builder()
                .name("자유게시판")
                .description("자유롭게 대화를 나누는 공간입니다")
                .build();
        
        Category qna = Category.builder()
                .name("Q&A")
                .description("질문과 답변을 나누는 공간입니다")
                .build();
        
        Category info = Category.builder()
                .name("정보")
                .description("유용한 정보 공유")
                .build();
        
        Category trading = Category.builder()
                .name("거래")
                .description("거래 관련 게시글")
                .build();
        
        // 루트 카테고리 저장
        categoryRepository.saveAll(List.of(notice, freeBoard, qna, info, trading));
        
        // 서브 카테고리 생성 및 연결
        Category coinNews = Category.builder()
                .name("코인 뉴스")
                .description("코인 관련 뉴스")
                .parent(info)
                .build();
        
        Category investmentStrategy = Category.builder()
                .name("투자 전략")
                .description("투자 전략 공유")
                .parent(info)
                .build();
        
        Category buy = Category.builder()
                .name("구매")
                .description("구매 관련 게시글")
                .parent(trading)
                .build();
        
        Category sell = Category.builder()
                .name("판매")
                .description("판매 관련 게시글")
                .parent(trading)
                .build();
        
        // 서브 카테고리 저장
        categoryRepository.saveAll(List.of(coinNews, investmentStrategy, buy, sell));
    }
    
    /**
     * 기본 카테고리 조회 (카테고리가 없을 경우 사용)
     * @return 기본 카테고리 또는 첫번째 카테고리
     */
    public Category getDefaultCategory() {
        // 기본적으로 '자유게시판' 카테고리 반환
        return categoryRepository.findByName("자유게시판")
                .orElseGet(() -> categoryRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("카테고리가 존재하지 않습니다.")));
    }
}
