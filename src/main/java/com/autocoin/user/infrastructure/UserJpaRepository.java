package com.autocoin.user.infrastructure;

import com.autocoin.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // 시스템 사용자 조회 - username이 'SYSTEM'인 사용자 조회
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
}
