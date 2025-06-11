package com.autocoin.post.infrastructure.repository;

import com.autocoin.category.domain.entity.Category;
import com.autocoin.post.domain.entity.Post;
import com.autocoin.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostJpaRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    List<Post> findByCategoryOrderByCreatedAtDesc(Category category);
    Page<Post> findByCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);
}
