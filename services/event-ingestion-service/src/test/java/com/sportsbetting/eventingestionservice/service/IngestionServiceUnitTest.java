package com.sportsbetting.eventingestionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.eventingestionservice.dto.ProviderEventRequest;
import com.sportsbetting.eventingestionservice.repository.NormalizedEventRepository;
import com.sportsbetting.eventingestionservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionServiceUnitTest {

    @Mock NormalizedEventRepository normalizedEventRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    @Test
    void ingestSavesNormalizedEventAndOutbox() {
        IngestionService service = new IngestionService(normalizedEventRepository, outboxEventRepository, new ObjectMapper());
        service.ingest(new ProviderEventRequest("demo", "SPORT_EVENT", Map.of("eventId", "e1")));

        verify(normalizedEventRepository).save(any());
        verify(outboxEventRepository).save(any());
    }
}
