package com.sportsbetting.settlementservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.settlementservice.dto.SettleEventResponse;
import com.sportsbetting.settlementservice.model.BetPositionEntity;
import com.sportsbetting.settlementservice.repository.BetPositionRepository;
import com.sportsbetting.settlementservice.repository.EventSettlementRepository;
import com.sportsbetting.settlementservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceUnitTest {

    @Mock
    private BetPositionRepository betPositionRepository;
    @Mock
    private EventSettlementRepository eventSettlementRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void settleEventComputesWinnersLosersPayoutAndPublishesOutbox() {
        when(eventSettlementRepository.findById("event-1")).thenReturn(Optional.empty());
        when(betPositionRepository.findByEventIdAndStatus("event-1", "OPEN"))
                .thenReturn(List.of(
                        open("bet-1", "u1", "HOME", "10.00", "2.00"),
                        open("bet-2", "u2", "AWAY", "5.00", "3.00")
                ));
        when(betPositionRepository.findAll()).thenReturn(List.of());

        SettlementService service = new SettlementService(
                betPositionRepository,
                eventSettlementRepository,
                outboxEventRepository,
                new ObjectMapper()
        );

        SettleEventResponse response = service.settleEvent("event-1", "HOME");

        assertThat(response.winners()).isEqualTo(1);
        assertThat(response.losers()).isEqualTo(1);
        assertThat(response.totalPayout()).isEqualByComparingTo("20.00");

        ArgumentCaptor<BetPositionEntity> betCaptor = ArgumentCaptor.forClass(BetPositionEntity.class);
        verify(betPositionRepository, atLeast(2)).save(betCaptor.capture());
        assertThat(betCaptor.getAllValues())
                .extracting(BetPositionEntity::getStatus)
                .contains("WON", "LOST");
        verify(outboxEventRepository).save(any());
    }

    private static BetPositionEntity open(String betId, String userId, String selection, String stake, String odds) {
        BetPositionEntity bet = new BetPositionEntity();
        bet.setBetId(betId);
        bet.setUserId(userId);
        bet.setEventId("event-1");
        bet.setSelection(selection);
        bet.setStake(new BigDecimal(stake));
        bet.setOdds(new BigDecimal(odds));
        bet.setStatus("OPEN");
        return bet;
    }
}
