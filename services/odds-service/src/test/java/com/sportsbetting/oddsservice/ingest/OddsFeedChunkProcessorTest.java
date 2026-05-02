package com.sportsbetting.oddsservice.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.outbox.OutboxEventFactory;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsFeedChunkProcessorTest {

    @Mock
    OddsQuoteRepository oddsQuoteRepository;
    @Mock
    OutboxEventRepository outboxEventRepository;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void processValidatesPersistsQuotesAndOutbox() {
        OddsUpdateValidator oddsUpdateValidator = new OddsUpdateValidator(validator);
        ObjectProvider<EventPayloadValidator> schemaProvider = mock(ObjectProvider.class);
        OutboxEventFactory factory = new OutboxEventFactory(testObjectMapper(), schemaProvider);
        OddsFeedChunkProcessor processor = new OddsFeedChunkProcessor(
                oddsQuoteRepository,
                outboxEventRepository,
                oddsUpdateValidator,
                factory
        );

        when(oddsQuoteRepository.findAllByKeyIdIn(List.of("event-1::HOME"))).thenReturn(Collections.emptyList());

        OddsUpdate update = new OddsUpdate("event-1", "HOME", new BigDecimal("2.109"));
        processor.process(List.of(update));

        verify(oddsQuoteRepository).saveAll(any());
        verify(outboxEventRepository).saveAll(argThat(iter ->
                iter instanceof Collection<?> c && c.size() == 1));
    }

    @Test
    void processRejectsInvalidUpdates() {
        OddsUpdateValidator oddsUpdateValidator = new OddsUpdateValidator(validator);
        OutboxEventFactory factory = new OutboxEventFactory(testObjectMapper(), mock(ObjectProvider.class));
        OddsFeedChunkProcessor processor = new OddsFeedChunkProcessor(
                oddsQuoteRepository,
                outboxEventRepository,
                oddsUpdateValidator,
                factory
        );

        assertThatThrownBy(() -> processor.process(List.of(new OddsUpdate("event-1", "HOME", BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
