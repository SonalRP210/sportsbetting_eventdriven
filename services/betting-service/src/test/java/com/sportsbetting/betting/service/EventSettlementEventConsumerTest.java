package com.sportsbetting.betting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.model.ProcessedEventEntity;
import com.sportsbetting.betting.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSettlementEventConsumerTest {

    @Mock
    private BettingService bettingService;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private EventSettlementEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EventSettlementEventConsumer(bettingService, processedEventRepository, new ObjectMapper());
    }

    @Test
    void onEventSettledAppliesProjectionAndPersistsProcessedKey() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        String payload = "{\"eventId\":\"event-42\",\"winningSelection\":\"HOME\",\"releases\":[]}";

        consumer.onEventSettled(payload, "settlement.event.settled.v1");

        verify(bettingService).applyEventSettlement("event-42", "HOME");
        ArgumentCaptor<ProcessedEventEntity> cap = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventKey()).startsWith("settlement.event.settled.v1:");
    }

    @Test
    void onEventSettledSkipsWhenAlreadyProcessed() {
        when(processedEventRepository.existsById(anyString())).thenReturn(true);

        consumer.onEventSettled("{\"eventId\":\"e1\",\"winningSelection\":\"AWAY\"}", "settlement.event.settled.v1");

        verify(bettingService, never()).applyEventSettlement(anyString(), anyString());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void invalidJsonThrowsIllegalArgument() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        assertThatThrownBy(() -> consumer.onEventSettled("not-json", "settlement.event.settled.v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid kafka payload");
    }
}
