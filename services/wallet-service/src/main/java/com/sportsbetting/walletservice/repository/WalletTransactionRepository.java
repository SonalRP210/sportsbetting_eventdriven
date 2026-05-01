package com.sportsbetting.walletservice.repository;

import com.sportsbetting.walletservice.model.WalletTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, String> {
}
