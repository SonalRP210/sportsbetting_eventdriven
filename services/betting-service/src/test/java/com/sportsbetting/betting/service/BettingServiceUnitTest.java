package com.sportsbetting.betting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.dto.PlaceBetRequest;
import com.sportsbetting.betting.dto.PlaceBetResponse;
import com.sportsbetting.betting.model.BetEntity;
import com.sportsbetting.betting.model.BetStatus;
import com.sportsbetting.betting.model.OddsQuoteEntity;
import com.sportsbetting.betting.repository.BetRepository;
import com.sportsbetting.betting.repository.OddsQuoteRepository;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BettingServiceUnitTest {

    @Mock
    private BetRepository betRepository;
    @Mock
    private OddsQuoteRepository oddsQuoteRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    @SuppressWarnings("unchecked")
    private ObjectProvider<EventPayloadValidator> eventSchemaValidator;

    private BettingService newService() {
        lenient().when(eventSchemaValidator.getIfAvailable()).thenReturn(null);
        return new BettingService(
                betRepository,
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                eventSchemaValidator
        );
    }

    @Test
    void placeBetReturnsReplayForIdempotencyKey() {
        BetEntity existing = new BetEntity();
        existing.setBetId("BET-EXISTING");
        existing.setOdds(new BigDecimal("1.90"));
        existing.setStatus(BetStatus.OPEN);

        when(betRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1"))
                .thenReturn(Optional.of(existing));

        PlaceBetResponse response = newService().placeBet(
                new PlaceBetRequest("user-1", "event-1", "HOME", new BigDecimal("10.00")),
                " idem-1 "
        );

        assertThat(response.betId()).isEqualTo("BET-EXISTING");
        assertThat(response.acceptedOdds()).isEqualByComparingTo("1.90");
        assertThat(response.status()).isEqualTo("OPEN");

        verify(oddsQuoteRepository, never()).findById(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void placeBetPersistsNormalizedStakeOddsAndOutbox() {
        OddsQuoteEntity quote = new OddsQuoteEntity();
        quote.setOdds(new BigDecimal("2.109"));
        when(oddsQuoteRepository.findById(eq("event-1::HOME"))).thenReturn(Optional.of(quote));

        PlaceBetResponse response = newService().placeBet(
                new PlaceBetRequest("user-1", "event-1", "HOME", new BigDecimal("10.005")),
                "idem-2"
        );

        assertThat(response.betId()).startsWith("BET-");
        assertThat(response.acceptedOdds()).isEqualByComparingTo("2.11");
        assertThat(response.status()).isEqualTo("OPEN");

        ArgumentCaptor<BetEntity> betCaptor = ArgumentCaptor.forClass(BetEntity.class);
        verify(betRepository).save(betCaptor.capture());
        BetEntity saved = betCaptor.getValue();
        assertThat(saved.getStake()).isEqualByComparingTo("10.01");
        assertThat(saved.getOdds()).isEqualByComparingTo("2.11");
        assertThat(saved.getIdempotencyKey()).isEqualTo("idem-2");

        verify(outboxEventRepository).save(any());
    }

    @Test
    void applyEventSettlementMarksWinnersAndLosers() {
        BetEntity home = new BetEntity();
        home.setBetId("BET-H");
        home.setUserId("u1");
        home.setEventId("event-001");
        home.setSelection("HOME");
        home.setStake(new BigDecimal("10.00"));
        home.setOdds(new BigDecimal("2.00"));
        home.setStatus(BetStatus.OPEN);

        BetEntity away = new BetEntity();
        away.setBetId("BET-A");
        away.setUserId("u2");
        away.setEventId("event-001");
        away.setSelection("AWAY");
        away.setStake(new BigDecimal("5.00"));
        away.setOdds(new BigDecimal("3.00"));
        away.setStatus(BetStatus.OPEN);

        when(betRepository.findByEventIdAndStatus("event-001", BetStatus.OPEN)).thenReturn(List.of(home, away));

        newService().applyEventSettlement("event-001", "HOME");

        assertThat(home.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(away.getStatus()).isEqualTo(BetStatus.LOST);
        verify(betRepository).save(home);
        verify(betRepository).save(away);
    }
}
