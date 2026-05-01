package com.sportsbetting.walletservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.walletservice.dto.WalletTransactionRequest;
import com.sportsbetting.walletservice.model.WalletAccountEntity;
import com.sportsbetting.walletservice.repository.OutboxEventRepository;
import com.sportsbetting.walletservice.repository.WalletAccountRepository;
import com.sportsbetting.walletservice.repository.WalletTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceUnitTest {

    @Mock WalletAccountRepository walletAccountRepository;
    @Mock WalletTransactionRepository walletTransactionRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    @Test
    void postTransactionCreditsBalanceAndPublishesOutbox() {
        WalletAccountEntity account = new WalletAccountEntity();
        account.setWalletId("wallet-1");
        account.setUserId("user-1");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("10.00"));
        when(walletAccountRepository.findById("wallet-1")).thenReturn(Optional.of(account));

        WalletService service = new WalletService(walletAccountRepository, walletTransactionRepository, outboxEventRepository, new ObjectMapper());
        var tx = service.postTransaction(new WalletTransactionRequest("wallet-1", "user-1", new BigDecimal("5.00"), "USD", "CREDIT", "r1"));

        assertThat(tx.amount()).isEqualByComparingTo("5.00");
        verify(walletAccountRepository).save(any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void postTransactionPreventsNegativeBalance() {
        WalletAccountEntity account = new WalletAccountEntity();
        account.setWalletId("wallet-1");
        account.setUserId("user-1");
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("1.00"));
        when(walletAccountRepository.findById("wallet-1")).thenReturn(Optional.of(account));

        WalletService service = new WalletService(walletAccountRepository, walletTransactionRepository, outboxEventRepository, new ObjectMapper());
        assertThatThrownBy(() -> service.postTransaction(new WalletTransactionRequest("wallet-1", "user-1", new BigDecimal("5.00"), "USD", "DEBIT", "r1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INSUFFICIENT_FUNDS");
    }
}
