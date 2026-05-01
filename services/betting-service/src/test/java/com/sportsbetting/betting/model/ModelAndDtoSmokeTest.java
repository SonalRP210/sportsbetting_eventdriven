package com.sportsbetting.betting.model;

import com.sportsbetting.betting.dto.UserBetSummaryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ModelAndDtoSmokeTest {

    @Test
    void betWithStatusCopiesFields() {
        Bet b = new Bet("b1", "u", "e", "H", BigDecimal.ONE, BigDecimal.TWO, BetStatus.OPEN, "ik");
        Bet c = b.withStatus(BetStatus.CANCELLED);
        assertThat(c.status()).isEqualTo(BetStatus.CANCELLED);
        assertThat(c.betId()).isEqualTo("b1");
    }

    @Test
    void userBetSummaryResponseAccessor() {
        UserBetSummaryResponse r = new UserBetSummaryResponse("b", "e", BigDecimal.TEN, BigDecimal.ONE, "OPEN");
        assertThat(r.betId()).isEqualTo("b");
        assertThat(r.odds()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void processedEventEntityMutators() {
        ProcessedEventEntity e = new ProcessedEventEntity();
        e.setEventKey("k");
        e.setProcessedAt(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(e.getEventKey()).isEqualTo("k");
        assertThat(e.getProcessedAt()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
    }
}
