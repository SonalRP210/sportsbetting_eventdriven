package com.sportsbetting.settlementservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.settlementservice.model.ProcessedEventEntity;
import com.sportsbetting.settlementservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementEventConsumerTest {

    @Mock
    private SettlementService settlementService;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private SettlementEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SettlementEventConsumer(settlementService, processedEventRepository, new ObjectMapper());
    }

    @Test
    void onBetPlacedDelegatesAndStoresIdempotencyMarker() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        consumer.onBetPlaced("{\"betId\":\"b1\",\"userId\":\"u1\",\"eventId\":\"e1\",\"selection\":\"HOME\",\"stake\":10,\"odds\":2.5}", "betting.bet.placed.v1");

        verify(settlementService).onBetPlaced("b1", "u1", "e1", "HOME", new BigDecimal("10"), new BigDecimal("2.5"));
        ArgumentCaptor<ProcessedEventEntity> cap = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventKey()).startsWith("betting.bet.placed.v1:");
    }

    @Test
    void onBetCancelledDelegates() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        consumer.onBetCancelled("{\"betId\":\"b2\"}", "betting.bet.cancelled.v1");
        verify(settlementService).onBetCancelled("b2");
    }

    @Test
    void duplicateEventIsIgnored() {
        when(processedEventRepository.existsById(anyString())).thenReturn(true);
        consumer.onBetCancelled("{\"betId\":\"b2\"}", "betting.bet.cancelled.v1");
        verify(settlementService, never()).onBetCancelled(anyString());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void invalidPayloadThrows() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        assertThatThrownBy(() -> consumer.onBetPlaced("bad", "topic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid kafka payload");
    }
}
