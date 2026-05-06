package com.sportsbetting.oddsservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.model.odds.OddsUpdatedEvent;
import com.sportsbetting.oddsservice.model.outbox.OutboxEventEntity;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventFactoryTest {

    private final ObjectMapper objectMapper = createTestObjectMapper();

    private static ObjectMapper createTestObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private final Instant fixed = Instant.parse("2026-05-02T12:00:00Z");

    @Mock
    ObjectProvider<EventPayloadValidator> schemaProvider;

    @Test
    void createPendingInvokesSchemaValidatorWhenAvailable() {
        EventPayloadValidator validator = mock(EventPayloadValidator.class);
        doAnswer(invocation -> {
            Consumer<EventPayloadValidator> action = invocation.getArgument(0);
            action.accept(validator);
            return null;
        }).when(schemaProvider).ifAvailable(any());

        OutboxEventFactory factory = new OutboxEventFactory(objectMapper, schemaProvider);
        OddsUpdatedEvent payload = OddsUpdatedEvent.from(
                new OddsUpdate("e", "HOME", new BigDecimal("2.10")),
                fixed,
                new BigDecimal("2.10")
        );

        factory.createPending(OddsUpdatedEvent.EVENT_TYPE, payload);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(validator).validateIfPresent(eq(OddsUpdatedEvent.EVENT_TYPE), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("eventId").contains("updatedAt");
    }

    @Test
    void createPendingPropagatesSchemaFailures() {
        EventPayloadValidator validator = mock(EventPayloadValidator.class);
        doThrow(new IllegalArgumentException("schema mismatch")).when(validator).validateIfPresent(anyString(), anyString());
        doAnswer(invocation -> {
            Consumer<EventPayloadValidator> action = invocation.getArgument(0);
            action.accept(validator);
            return null;
        }).when(schemaProvider).ifAvailable(any());

        OutboxEventFactory factory = new OutboxEventFactory(objectMapper, schemaProvider);
        OddsUpdatedEvent payload = OddsUpdatedEvent.from(
                new OddsUpdate("e", "HOME", new BigDecimal("2.10")),
                fixed,
                new BigDecimal("2.10")
        );

        assertThatThrownBy(() -> factory.createPending(OddsUpdatedEvent.EVENT_TYPE, payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schema mismatch");
    }

    @Test
    void createPendingSkipsValidationWhenValidatorBeanAbsent() {
        ObjectProvider<EventPayloadValidator> emptyProvider = mock(ObjectProvider.class);
        OutboxEventFactory factory = new OutboxEventFactory(objectMapper, emptyProvider);
        OddsUpdatedEvent payload = OddsUpdatedEvent.from(
                new OddsUpdate("e", "HOME", new BigDecimal("2.10")),
                fixed,
                new BigDecimal("2.10")
        );

        OutboxEventEntity row = factory.createPending(OddsUpdatedEvent.EVENT_TYPE, payload);

        assertThat(row.getEventType()).isEqualTo(OddsUpdatedEvent.EVENT_TYPE);
        assertThat(row.isPublished()).isFalse();
    }
}
