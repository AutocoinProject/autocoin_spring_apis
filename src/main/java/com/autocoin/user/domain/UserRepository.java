package com.autocoin.user.domain;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByUsername(String username); // 시스템 사용자 조회 (익명 게시물용)
    boolean existsByEmail(String email);
    void delete(User user);
}
