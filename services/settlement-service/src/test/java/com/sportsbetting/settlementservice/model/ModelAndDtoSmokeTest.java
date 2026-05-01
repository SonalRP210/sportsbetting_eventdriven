package com.sportsbetting.settlementservice.model;

import com.sportsbetting.settlementservice.dto.SettleEventRequest;
import com.sportsbetting.settlementservice.dto.SettleEventResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModelAndDtoSmokeTest {

    @Test
    void recordsAndMutatorsWork() {
        BetPosition b = new BetPosition("b1", "u1", "e1", "HOME", BigDecimal.ONE, BigDecimal.TEN, "OPEN");
        assertThat(b.settled("WON").status()).isEqualTo("WON");

        EventSettlement es = new EventSettlement("e1", "HOME", 1, 2, BigDecimal.TEN);
        assertThat(es.winningSelection()).isEqualTo("HOME");

        DomainEvent de = new DomainEvent("type", Map.of("k", "v"));
        assertThat(de.type()).isEqualTo("type");

        SettleEventRequest req = new SettleEventRequest("e1", "HOME");
        assertThat(req.eventId()).isEqualTo("e1");

        SettleEventResponse res = new SettleEventResponse("e1", "HOME", 1, 0, BigDecimal.TEN, BigDecimal.ZERO);
        assertThat(res.totalPayout()).isEqualByComparingTo("10");
    }

    @Test
    void entitiesExposeProperties() {
        ProcessedEventEntity pe = new ProcessedEventEntity();
        pe.setEventKey("k1");
        pe.setProcessedAt(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(pe.getEventKey()).isEqualTo("k1");

        OutboxEventEntity oe = new OutboxEventEntity();
        oe.setId(UUID.randomUUID());
        oe.setEventType("evt");
        oe.setPayload("{}");
        oe.setPublished(false);
        oe.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
        oe.setPublishedAt(Instant.parse("2020-01-01T00:00:01Z"));
        assertThat(oe.getEventType()).isEqualTo("evt");

        BetPositionEntity bpe = new BetPositionEntity();
        bpe.setBetId("b1");
        bpe.setUserId("u1");
        bpe.setEventId("e1");
        bpe.setSelection("HOME");
        bpe.setStake(BigDecimal.ONE);
        bpe.setOdds(BigDecimal.TEN);
        bpe.setStatus("OPEN");
        assertThat(bpe.getStatus()).isEqualTo("OPEN");

        EventSettlementEntity ese = new EventSettlementEntity();
        ese.setEventId("e1");
        ese.setWinningSelection("HOME");
        ese.setWinners(1);
        ese.setLosers(2);
        ese.setTotalPayout(BigDecimal.TEN);
        assertThat(ese.getWinners()).isEqualTo(1);
    }
}
