package com.autocoin.news.infrastructure.repository;

import com.autocoin.news.domain.entity.News;
import com.autocoin.news.domain.enums.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsJpaRepository extends JpaRepository<News, Long> {
    
    /**
     * 카테고리별 뉴스 조회 (페이징)
     */
    Page<News> findByCategory(NewsCategory category, Pageable pageable);
    
    /**
     * 카테고리별 뉴스 조회 (발행일 내림차순)
     */
    Page<News> findByCategoryOrderByPublishedAtDesc(NewsCategory category, Pageable pageable);
    
    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);
    
    /**
     * 최신 뉴스 10개 조회
     */
    List<News> findTop10ByOrderByPublishedAtDesc();
    
    /**
     * 발행일 내림차순으로 모든 뉴스 조회 (페이징)
     */
    Page<News> findByOrderByPublishedAtDesc(Pageable pageable);
    
    /**
     * 특정 시간 이전 뉴스 조회
     */
    List<News> findByPublishedAtBefore(LocalDateTime dateTime);
    
    /**
     * 특정 시간 이전 뉴스 삭제
     */
    void deleteByPublishedAtBefore(LocalDateTime dateTime);
    
    /**
     * 카테고리별 뉴스 개수 조회
     */
    Long countByCategory(NewsCategory category);
    
    /**
     * 조회수 상위 5개 뉴스 조회
     */
    List<News> findTop5ByOrderByViewCountDesc();
    
    /**
     * 제목 또는 설명 검색 (페이징)
     */
    Page<News> findByTitleContainingOrDescriptionContainingOrderByPublishedAtDesc(String title, String description, Pageable pageable);
    
    /**
     * 특정 날짜 이후 뉴스 개수 조회
     */
    Long countByPublishedAtAfter(LocalDateTime date);
}
