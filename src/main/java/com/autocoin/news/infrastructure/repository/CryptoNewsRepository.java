package com.autocoin.news.infrastructure.repository;

import com.autocoin.news.domain.entity.CryptoNews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoNewsRepository extends JpaRepository<CryptoNews, Long> {
    
    Optional<CryptoNews> findByLink(String link);
    
    boolean existsByLink(String link);
    
    /**
     * 가장 오래된 뉴스를 조회합니다.
     * @param pageable 페이징 정보
     * @return 오래된 뉴스 리스트
     */
    @Query("SELECT n FROM CryptoNews n ORDER BY n.createdAt ASC")
    List<CryptoNews> findOldestNews(Pageable pageable);
}
