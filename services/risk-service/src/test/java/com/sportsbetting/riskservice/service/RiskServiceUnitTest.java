package com.sportsbetting.riskservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.riskservice.dto.BetCancelledEventRequest;
import com.sportsbetting.riskservice.dto.BetPlacedEventRequest;
import com.sportsbetting.riskservice.dto.EventSettledRequest;
import com.sportsbetting.riskservice.repository.OutboxEventRepository;
import com.sportsbetting.riskservice.repository.UserExposureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskServiceUnitTest {

    @Mock UserExposureRepository userExposureRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    @Test
    void riskFlowUpdatesAndPublishesOutbox() {
        RiskService service = new RiskService(userExposureRepository, outboxEventRepository, new ObjectMapper());

        service.onBetPlaced(new BetPlacedEventRequest("b1", "u1", new BigDecimal("12.50")));
        service.onBetCancelled(new BetCancelledEventRequest("b1", "u1", new BigDecimal("5.00")));
        service.onEventSettled(new EventSettledRequest("e1", "HOME", List.of(new EventSettledRequest.RiskRelease("u1", new BigDecimal("2.00")))));

        verify(userExposureRepository, atLeast(3)).save(any());
        verify(outboxEventRepository, atLeast(3)).save(any());
    }
}
