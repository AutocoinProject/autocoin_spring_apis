package com.autocoin.post.domain;

import com.autocoin.category.domain.entity.Category;
import com.autocoin.post.domain.entity.Post;
import java.util.List;
import com.autocoin.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Post 도메인의 Repository 인터페이스 명세
 * 구현체는 infrastructure 계층에 위치
 */
public interface PostRepository {
    // CRUD 기본 기능
    Post save(Post post);
    Optional<Post> findById(Long id);
    
    // 리스트 조회 (내림차순)
    List<Post> findAllByOrderByCreatedAtDesc();
    
    // 카테고리별 게시글 목록 조회 (페이징)
    Page<Post> findByCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);
    
    // 카테고리별 게시글 목록 조회
    List<Post> findByCategoryOrderByCreatedAtDesc(Category category);
    
    // 사용자별 게시글 조회
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    
    // 게시글 삭제
    void delete(Post post);
}
