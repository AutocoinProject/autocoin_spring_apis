package com.autocoin.upbit.domain;

import com.autocoin.upbit.domain.entity.UpbitAccount;
import com.autocoin.user.domain.User;

import java.util.Optional;

public interface UpbitAccountRepository {
    Optional<UpbitAccount> findByUser(User user);
    Optional<UpbitAccount> findById(Long id);
    UpbitAccount save(UpbitAccount upbitAccount);
    void deleteByUser(User user);
    boolean existsByUser(User user);
}