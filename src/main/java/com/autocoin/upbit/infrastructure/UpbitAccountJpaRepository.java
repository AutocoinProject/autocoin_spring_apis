package com.autocoin.upbit.infrastructure;

import com.autocoin.upbit.domain.entity.UpbitAccount;
import com.autocoin.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UpbitAccountJpaRepository extends JpaRepository<UpbitAccount, Long> {
    Optional<UpbitAccount> findByUser(User user);
    void deleteByUser(User user);
    boolean existsByUser(User user);
}