package com.sportsbetting.oddsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsServiceUnitTest {

    @Mock
    OddsQuoteRepository oddsQuoteRepository;
    @Mock
    OutboxEventRepository outboxEventRepository;
    @Mock
    TransactionTemplate transactionTemplate;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void stubTransactionTemplate() {
        lenient().doAnswer(inv -> {
            Consumer<TransactionStatus> consumer = inv.getArgument(0);
            consumer.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void consumeOddsFeedPersistsNormalizedQuoteAndOutbox() {
        when(oddsQuoteRepository.findAllByKeyIdIn(List.of("event-1::HOME"))).thenReturn(Collections.emptyList());
        ObjectProvider<EventPayloadValidator> schemaProvider = mock(ObjectProvider.class);
        OddsService service = new OddsService(
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                transactionTemplate,
                schemaProvider,
                validator,
                50
        );

        service.consumeOddsFeed(List.of(new OddsUpdate("event-1", "HOME", new BigDecimal("2.109"))));

        verify(oddsQuoteRepository).saveAll(any());
        verify(outboxEventRepository).saveAll(any());
    }

    @Test
    void consumeOddsFeedRunsOneTransactionPerChunk() {
        when(oddsQuoteRepository.findAllByKeyIdIn(any())).thenReturn(Collections.emptyList());
        ObjectProvider<EventPayloadValidator> schemaProvider = mock(ObjectProvider.class);
        OddsService service = new OddsService(
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                transactionTemplate,
                schemaProvider,
                validator,
                1
        );

        service.consumeOddsFeed(List.of(
                new OddsUpdate("event-1", "HOME", new BigDecimal("2.10")),
                new OddsUpdate("event-2", "AWAY", new BigDecimal("3.10"))
        ));

        verify(transactionTemplate, times(2)).executeWithoutResult(any());
    }

    @Test
    void consumeOddsFeedRejectsInvalidInput() {
        ObjectProvider<EventPayloadValidator> schemaProvider = mock(ObjectProvider.class);
        OddsService service = new OddsService(
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                transactionTemplate,
                schemaProvider,
                validator,
                50
        );

        assertThatThrownBy(() -> service.consumeOddsFeed(List.of(new OddsUpdate("event-1", "HOME", BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
