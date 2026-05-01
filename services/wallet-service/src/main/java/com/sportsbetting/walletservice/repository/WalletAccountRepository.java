package com.sportsbetting.walletservice.repository;

import com.sportsbetting.walletservice.model.WalletAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccountEntity, String> {
    Optional<WalletAccountEntity> findByUserId(String userId);
}
