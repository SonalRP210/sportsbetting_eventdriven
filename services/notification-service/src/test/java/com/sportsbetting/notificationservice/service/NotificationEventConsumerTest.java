package com.sportsbetting.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.notificationservice.model.ProcessedEventEntity;
import com.sportsbetting.notificationservice.repository.ProcessedEventRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationEventConsumer(notificationService, processedEventRepository, new ObjectMapper());
    }

    @Test
    void onEventDelegatesAndStoresMarker() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        consumer.onEvent("{\"eventType\":\"betting.bet.placed.v1\",\"userId\":\"u1\",\"amount\":10}", "betting.bet.placed.v1");

        verify(notificationService).consume(any());
        ArgumentCaptor<ProcessedEventEntity> cap = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventKey()).startsWith("betting.bet.placed.v1:");
    }

    @Test
    void duplicateEventIsIgnored() {
        when(processedEventRepository.existsById(anyString())).thenReturn(true);
        consumer.onEvent("{\"eventType\":\"x\"}", "x");
        verify(notificationService, never()).consume(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void invalidPayloadThrows() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        assertThatThrownBy(() -> consumer.onEvent("bad", "topic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid kafka payload");
    }
}
