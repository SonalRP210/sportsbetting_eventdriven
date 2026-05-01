package com.sportsbetting.riskservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.riskservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskEventConsumerContractTest {

    @Mock
    private RiskService riskService;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Test
    void consumerAcceptsBackwardCompatiblePayloadWithExtraFields() {
        when(processedEventRepository.existsById(any())).thenReturn(false);
        RiskEventConsumer consumer = new RiskEventConsumer(riskService, processedEventRepository, new ObjectMapper());

        consumer.onBetPlaced(
                "{\"betId\":\"BET-1\",\"userId\":\"user-1\",\"openRisk\":12.50,\"newField\":\"ignored\"}",
                "betting.bet.placed.v1"
        );

        verify(riskService).onBetPlaced(any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void duplicateEventIsIgnored() {
        when(processedEventRepository.existsById(any())).thenReturn(true);
        RiskEventConsumer consumer = new RiskEventConsumer(riskService, processedEventRepository, new ObjectMapper());

        consumer.onBetPlaced(
                "{\"betId\":\"BET-1\",\"userId\":\"user-1\",\"openRisk\":12.50}",
                "betting.bet.placed.v1"
        );

        verify(riskService, never()).onBetPlaced(any());
        verify(processedEventRepository, never()).save(any());
    }
}
