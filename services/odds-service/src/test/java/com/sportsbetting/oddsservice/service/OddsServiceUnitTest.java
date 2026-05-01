package com.sportsbetting.oddsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsServiceUnitTest {

    @Mock
    OddsQuoteRepository oddsQuoteRepository;
    @Mock
    OutboxEventRepository outboxEventRepository;

    @Test
    void consumeOddsFeedPersistsNormalizedQuoteAndOutbox() {
        when(oddsQuoteRepository.findById("event-1::HOME")).thenReturn(Optional.empty());
        OddsService service = new OddsService(oddsQuoteRepository, outboxEventRepository, new ObjectMapper());

        service.consumeOddsFeed(List.of(new OddsUpdate("event-1", "HOME", new BigDecimal("2.109"))));

        verify(oddsQuoteRepository).save(any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void consumeOddsFeedRejectsInvalidInput() {
        OddsService service = new OddsService(oddsQuoteRepository, outboxEventRepository, new ObjectMapper());
        assertThatThrownBy(() -> service.consumeOddsFeed(List.of(new OddsUpdate("event-1", "HOME", BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
