package com.autocoin.upbit.infrastructure;

import com.autocoin.upbit.domain.UpbitAccountRepository;
import com.autocoin.upbit.domain.entity.UpbitAccount;
import com.autocoin.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UpbitAccountRepositoryImpl implements UpbitAccountRepository {
    
    private final UpbitAccountJpaRepository upbitAccountJpaRepository;
    
    @Override
    public Optional<UpbitAccount> findByUser(User user) {
        return upbitAccountJpaRepository.findByUser(user);
    }
    
    @Override
    public Optional<UpbitAccount> findById(Long id) {
        return upbitAccountJpaRepository.findById(id);
    }
    
    @Override
    public UpbitAccount save(UpbitAccount upbitAccount) {
        return upbitAccountJpaRepository.save(upbitAccount);
    }
    
    @Override
    public void deleteByUser(User user) {
        upbitAccountJpaRepository.deleteByUser(user);
    }
    
    @Override
    public boolean existsByUser(User user) {
        return upbitAccountJpaRepository.existsByUser(user);
    }
}