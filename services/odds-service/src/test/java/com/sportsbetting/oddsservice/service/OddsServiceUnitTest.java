package com.sportsbetting.oddsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.ingest.OddsFeedChunkProcessor;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OddsServiceUnitTest {

    @Mock
    OddsQuoteRepository oddsQuoteRepository;
    @Mock
    OutboxEventRepository outboxEventRepository;
    @Mock
    OddsFeedChunkProcessor oddsFeedChunkProcessor;
    @Mock
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void stubTransactionTemplate() {
        lenient().doAnswer(inv -> {
            Consumer<TransactionStatus> consumer = inv.getArgument(0);
            consumer.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void consumeOddsFeedDelegatesChunksToProcessor() {
        OddsService service = new OddsService(
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                transactionTemplate,
                oddsFeedChunkProcessor,
                50
        );

        service.consumeOddsFeed(List.of(new OddsUpdate("event-1", "HOME", new BigDecimal("2.109"))));

        verify(oddsFeedChunkProcessor).process(List.of(new OddsUpdate("event-1", "HOME", new BigDecimal("2.109"))));
    }

    @Test
    void consumeOddsFeedRunsOneTransactionPerChunk() {
        OddsService service = new OddsService(
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                transactionTemplate,
                oddsFeedChunkProcessor,
                1
        );

        service.consumeOddsFeed(List.of(
                new OddsUpdate("event-1", "HOME", new BigDecimal("2.10")),
                new OddsUpdate("event-2", "AWAY", new BigDecimal("3.10"))
        ));

        verify(transactionTemplate, times(2)).executeWithoutResult(any());
        verify(oddsFeedChunkProcessor, times(2)).process(any());
    }

    @Test
    void consumeOddsFeedPropagatesFailuresFromChunkProcessor() {
        OddsService service = new OddsService(
                oddsQuoteRepository,
                outboxEventRepository,
                new ObjectMapper(),
                transactionTemplate,
                oddsFeedChunkProcessor,
                50
        );

        org.mockito.Mockito.doThrow(new IllegalArgumentException("chunk failure"))
                .when(oddsFeedChunkProcessor).process(any());

        assertThatThrownBy(() -> service.consumeOddsFeed(List.of(new OddsUpdate("event-1", "HOME", new BigDecimal("2.10")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunk failure");
    }
}
