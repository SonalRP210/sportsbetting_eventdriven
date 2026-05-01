package com.sportsbetting.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import com.sportsbetting.notificationservice.repository.NotificationDeliveryRepository;
import com.sportsbetting.notificationservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    @Test
    void consumePersistsDeliveryAndOutbox() {
        NotificationService service = new NotificationService(deliveryRepository, outboxEventRepository, new ObjectMapper());
        service.consume(new NotificationEventRequest("betting.bet.placed.v1", "u1", Map.of("betId", "b1")));

        verify(deliveryRepository).save(any());
        verify(outboxEventRepository).save(any());
    }
}
