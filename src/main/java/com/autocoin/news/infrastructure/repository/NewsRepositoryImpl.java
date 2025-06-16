package com.autocoin.news.infrastructure.repository;

import com.autocoin.news.domain.NewsRepository;
import com.autocoin.news.domain.entity.News;
import com.autocoin.news.domain.enums.NewsCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository {
    
    private final NewsJpaRepository newsJpaRepository;
    
    @Override
    public List<News> findAll() {
        return newsJpaRepository.findAll();
    }
    
    @Override
    public Page<News> findAll(Pageable pageable) {
        return newsJpaRepository.findAll(pageable);
    }
    
    @Override
    public List<News> findByCategory(NewsCategory category) {
        return newsJpaRepository.findByCategory(category, Pageable.unpaged()).getContent();
    }
    
    @Override
    public Page<News> findByCategory(NewsCategory category, Pageable pageable) {
        return newsJpaRepository.findByCategory(category, pageable);
    }
    
    @Override
    public Optional<News> findById(Long id) {
        return newsJpaRepository.findById(id);
    }
    
    @Override
    public News save(News news) {
        return newsJpaRepository.save(news);
    }
    
    @Override
    public void deleteById(Long id) {
        newsJpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByUrl(String url) {
        return newsJpaRepository.existsByUrl(url);
    }
    
    @Override
    public List<News> findTop10ByOrderByPublishedAtDesc() {
        return newsJpaRepository.findTop10ByOrderByPublishedAtDesc();
    }
    
    @Override
    public Page<News> findByOrderByPublishedAtDesc(Pageable pageable) {
        return newsJpaRepository.findByOrderByPublishedAtDesc(pageable);
    }
    
    @Override
    public Page<News> findByCategoryOrderByPublishedAtDesc(NewsCategory category, Pageable pageable) {
        return newsJpaRepository.findByCategoryOrderByPublishedAtDesc(category, pageable);
    }
    
    @Override
    public List<News> findByPublishedAtBefore(LocalDateTime dateTime) {
        return newsJpaRepository.findByPublishedAtBefore(dateTime);
    }
    
    @Override
    public void deleteByPublishedAtBefore(LocalDateTime dateTime) {
        newsJpaRepository.deleteByPublishedAtBefore(dateTime);
    }
    
    @Override
    public Long countByCategory(NewsCategory category) {
        return newsJpaRepository.countByCategory(category);
    }
    
    @Override
    public List<News> findTop5ByOrderByViewCountDesc() {
        return newsJpaRepository.findTop5ByOrderByViewCountDesc();
    }
}