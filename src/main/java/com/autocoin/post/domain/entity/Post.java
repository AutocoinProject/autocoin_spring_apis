package com.autocoin.post.domain.entity;

import com.autocoin.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.autocoin.category.domain.entity.Category;
import com.autocoin.user.domain.User;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String writer;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true) // user_id 필드는 NULL 허용
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    private String fileUrl;
    
    private String fileName;
    
    private String fileKey;

    // 수정 메서드
    public void update(String title, String content, String fileUrl, String fileName, String fileKey, String writer, Category category) {
        this.title = title;
        this.content = content;
        this.writer = writer;
        
        // 새 파일이 업로드된 경우에만 업데이트
        if (fileUrl != null && !fileUrl.isEmpty()) {
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.fileKey = fileKey;
        }
        
        // 카테고리가 지정된 경우에만 업데이트
        if (category != null) {
            this.category = category;
        }
    }
}
