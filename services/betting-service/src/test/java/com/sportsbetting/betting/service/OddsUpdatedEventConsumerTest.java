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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsUpdatedEventConsumerTest {

    @Mock
    private BettingService bettingService;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private OddsUpdatedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OddsUpdatedEventConsumer(bettingService, processedEventRepository, new ObjectMapper());
    }

    @Test
    void onOddsUpdatedInvokesBettingServiceAndPersistsProcessedKey() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        consumer.onOddsUpdated("{\"eventId\":\"e1\",\"selection\":\"HOME\",\"odds\":2.5}", "odds.topic");

        verify(bettingService).setOdds("e1", "HOME", new BigDecimal("2.5"));
        ArgumentCaptor<ProcessedEventEntity> cap = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventKey()).startsWith("odds.topic:");
    }

    @Test
    void onOddsUpdatedSkipsWhenAlreadyProcessed() {
        when(processedEventRepository.existsById(anyString())).thenReturn(true);

        consumer.onOddsUpdated("{\"eventId\":\"e1\",\"selection\":\"HOME\",\"odds\":1}", "t");

        verify(bettingService, never()).setOdds(anyString(), anyString(), any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void invalidJsonThrowsIllegalArgument() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        assertThatThrownBy(() -> consumer.onOddsUpdated("not-json", "t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid kafka payload");
    }
}
