package com.sportsbetting.walletservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.walletservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletService walletService;

    @BeforeEach
    void resetState() {
        walletService.resetForTests();
    }

    @Test
    void creditThenDebitUpdatesBalance() throws Exception {
        String credit = objectMapper.writeValueAsString(Map.of(
                "walletId", "wallet-user-1",
                "userId", "user-1",
                "amount", 200,
                "currency", "USD",
                "type", "CREDIT",
                "reference", "topup"
        ));

        String debit = objectMapper.writeValueAsString(Map.of(
                "walletId", "wallet-user-1",
                "userId", "user-1",
                "amount", 75,
                "currency", "USD",
                "type", "DEBIT",
                "reference", "bet"
        ));

        mockMvc.perform(post("/api/v1/wallet/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credit))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallet/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debit))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wallet/user-1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(125.00)));
    }

    @Test
    void insufficientFundsReturnsBadRequest() throws Exception {
        String debit = objectMapper.writeValueAsString(Map.of(
                "walletId", "wallet-user-2",
                "userId", "user-2",
                "amount", 50,
                "currency", "USD",
                "type", "DEBIT",
                "reference", "bet"
        ));

        mockMvc.perform(post("/api/v1/wallet/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_FUNDS")));
    }

    @Test
    void settlementEventCreditsWalletsAndPublishesEvents() throws Exception {
        String settlement = objectMapper.writeValueAsString(Map.of(
                "eventId", "event-001",
                "winningSelection", "HOME",
                "releases", List.of(
                        Map.of("userId", "user-a", "openRisk", 120.50),
                        Map.of("userId", "user-b", "openRisk", 80.00)
                )
        ));

        mockMvc.perform(post("/api/v1/internal/settlement-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settlement))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.posted", is(2)));

        mockMvc.perform(get("/api/v1/wallet/user-a/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(120.50)));

        mockMvc.perform(get("/api/v1/internal/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()", is(2)))
                .andExpect(jsonPath("$.events[0].type", is("wallet.wallet.credited.v1")));
    }
}
