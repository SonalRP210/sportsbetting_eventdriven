package com.sportsbetting.betting.outbox;

import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publishes a slice of outbox rows to Kafka and marks them published in the same transaction.
 * Used only when {@code app.outbox.poll.enabled=true} (polling fallback). When Debezium CDC
 * relays inserts, rows are not marked published and are pruned by {@link OutboxCleanup}.
 */
@Service
public class OutboxSlicePublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectProvider<EventPayloadValidator> eventSchemaValidator;

    public OutboxSlicePublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectProvider<EventPayloadValidator> eventSchemaValidator
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventSchemaValidator = eventSchemaValidator;
    }

    @Transactional
    public void publishSlice(List<OutboxEventEntity> slice) {
        Instant publishedAt = Instant.now();
        for (OutboxEventEntity event : slice) {
            eventSchemaValidator.ifAvailable(v -> v.validateIfPresent(event.getEventType(), event.getPayload()));
            String key = event.getMessageKey() != null ? event.getMessageKey() : event.getId().toString();
            kafkaTemplate.send(event.getEventType(), key, event.getPayload());
            event.setPublished(true);
            event.setPublishedAt(publishedAt);
        }
        outboxEventRepository.saveAll(slice);
    }
}
