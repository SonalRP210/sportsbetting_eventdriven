package com.sportsbetting.betting.repository;

import com.sportsbetting.betting.model.BetEntity;
import com.sportsbetting.betting.model.BetStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BetRepositoryDataJpaTest {

    @Autowired
    private BetRepository betRepository;

    @Test
    void findersReturnSortedAndIdempotentRecords() {
        betRepository.save(newBet("BET-003", "user-1", "event-1", "idem-1"));
        betRepository.save(newBet("BET-001", "user-1", "event-2", "idem-2"));
        betRepository.save(newBet("BET-002", "user-2", "event-1", null));

        List<BetEntity> byUser = betRepository.findByUserIdOrderByBetIdAsc("user-1");
        assertThat(byUser).extracting(BetEntity::getBetId).containsExactly("BET-001", "BET-003");

        List<BetEntity> byEvent = betRepository.findByEventIdOrderByBetIdAsc("event-1");
        assertThat(byEvent).extracting(BetEntity::getBetId).containsExactly("BET-002", "BET-003");

        assertThat(betRepository.findByUserIdAndIdempotencyKey("user-1", "idem-2"))
                .isPresent()
                .get()
                .extracting(BetEntity::getBetId)
                .isEqualTo("BET-001");

        assertThat(betRepository.findByEventIdAndStatus("event-1", BetStatus.OPEN))
                .extracting(BetEntity::getBetId)
                .containsExactlyInAnyOrder("BET-002", "BET-003");
    }

    private static BetEntity newBet(String betId, String userId, String eventId, String idempotencyKey) {
        BetEntity bet = new BetEntity();
        bet.setBetId(betId);
        bet.setUserId(userId);
        bet.setEventId(eventId);
        bet.setSelection("HOME");
        bet.setStake(new BigDecimal("10.00"));
        bet.setOdds(new BigDecimal("2.00"));
        bet.setStatus(BetStatus.OPEN);
        bet.setIdempotencyKey(idempotencyKey);
        return bet;
    }
}
