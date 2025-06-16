package com.autocoin.user.infrastructure;

import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }
    
    @Override
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        return userJpaRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<User> systemUser = userJpaRepository.findByUsername(username);
        
        // 시스템 사용자가 없으면 생성
        if (systemUser.isEmpty() && "SYSTEM".equals(username)) {
            return createSystemUser();
        }
        
        return systemUser;
    }
    
    /**
     * 시스템 사용자 생성 (엑세스 제한을 위해 private 메소드로 구현)
     */
    private Optional<User> createSystemUser() {
        try {
            // 시스템 사용자 생성
            User systemUser = User.builder()
                    .username("SYSTEM")
                    .email("system@autocoin.com")
                    .password("")
                    .role(com.autocoin.user.domain.Role.SYSTEM) // 시스템 사용자를 위한 역할
                    .build();
            
            return Optional.of(userJpaRepository.save(systemUser));
        } catch (Exception e) {
            // 동시에 여러 스레드에서 생성하려고 할 경우 예외 발생 가능
            // 이 경우 다시 조회 시도
            return userJpaRepository.findByUsername("SYSTEM");
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }
    
    @Override
    public void delete(User user) {
        userJpaRepository.delete(user);
    }
}
