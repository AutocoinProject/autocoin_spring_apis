package com.autocoin.upbit.domain.entity;

import com.autocoin.global.domain.BaseEntity;
import com.autocoin.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "upbit_accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpbitAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessKey;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedSecretKey;
    
    @Enumerated(EnumType.STRING)
    private AccountState accountState;
    
    private LocalDateTime lastSyncAt;
    
    private String nickname; // 업비트 계정 별명
    
    // 연결 상태
    public enum AccountState {
        ACTIVE, INACTIVE, ERROR, EXPIRED
    }
    
    public void updateApiKeys(String encryptedAccessKey, String encryptedSecretKey) {
        this.encryptedAccessKey = encryptedAccessKey;
        this.encryptedSecretKey = encryptedSecretKey;
        this.accountState = AccountState.ACTIVE;
        this.lastSyncAt = LocalDateTime.now();
    }
    
    public void updateAccountState(AccountState state) {
        this.accountState = state;
        this.lastSyncAt = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.accountState = AccountState.INACTIVE;
    }
    
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public boolean isActive() {
        return AccountState.ACTIVE.equals(this.accountState);
    }
}