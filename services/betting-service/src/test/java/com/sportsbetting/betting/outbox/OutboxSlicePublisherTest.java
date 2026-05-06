package com.sportsbetting.betting.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxSlicePublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void publishSliceSendsWithMessageKeyAsKafkaKey() {
        @SuppressWarnings("unchecked")
        ObjectProvider<EventPayloadValidator> validator = mock(ObjectProvider.class);
        lenient().when(validator.getIfAvailable()).thenReturn(null);

        OutboxSlicePublisher publisher = new OutboxSlicePublisher(outboxEventRepository, kafkaTemplate, validator);

        OutboxEventEntity row = new OutboxEventEntity();
        row.setId(UUID.randomUUID());
        row.setEventType("betting.bet.placed.v1");
        row.setPayload("{\"betId\":\"BET-KEY\",\"userId\":\"u\",\"eventId\":\"e\",\"selection\":\"H\",\"stake\":1,\"odds\":2,\"openRisk\":2}");
        row.setMessageKey("BET-KEY");
        row.setPublished(false);
        row.setCreatedAt(Instant.now());

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishSlice(List.of(row));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("betting.bet.placed.v1"), keyCaptor.capture(), anyString());
        assertThat(keyCaptor.getValue()).isEqualTo("BET-KEY");
        verify(outboxEventRepository).saveAll(any());
    }

    @Test
    void publishSliceFallsBackToRowIdWhenMessageKeyMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<EventPayloadValidator> validator = mock(ObjectProvider.class);
        lenient().when(validator.getIfAvailable()).thenReturn(null);

        OutboxSlicePublisher publisher = new OutboxSlicePublisher(outboxEventRepository, kafkaTemplate, validator);

        UUID id = UUID.randomUUID();
        OutboxEventEntity row = new OutboxEventEntity();
        row.setId(id);
        row.setEventType("betting.bet.placed.v1");
        row.setPayload("{\"betId\":\"BET-X\",\"userId\":\"u\",\"eventId\":\"e\",\"selection\":\"H\",\"stake\":1,\"odds\":2,\"openRisk\":2}");
        row.setMessageKey(null);
        row.setPublished(false);
        row.setCreatedAt(Instant.now());

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishSlice(List.of(row));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("betting.bet.placed.v1"), keyCaptor.capture(), anyString());
        assertThat(keyCaptor.getValue()).isEqualTo(id.toString());
    }

    @Test
    void publishSliceWithSchemaValidatorRejectsInvalidPayload() {
        ObjectMapper mapper = new ObjectMapper();
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(EventPayloadValidator.class, () -> new EventPayloadValidator(mapper));
            ctx.refresh();
            ObjectProvider<EventPayloadValidator> provider = ctx.getBeanProvider(EventPayloadValidator.class);

            OutboxSlicePublisher publisher = new OutboxSlicePublisher(outboxEventRepository, kafkaTemplate, provider);

            OutboxEventEntity row = new OutboxEventEntity();
            row.setId(UUID.randomUUID());
            row.setEventType("betting.bet.placed.v1");
            row.setPayload("{}");
            row.setMessageKey("BET-1");
            row.setPublished(false);
            row.setCreatedAt(Instant.now());

            assertThatThrownBy(() -> publisher.publishSlice(List.of(row)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("schema");

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }
}
