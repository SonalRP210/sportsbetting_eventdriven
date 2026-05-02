package com.sportsbetting.oddsservice.outbox;

import com.sportsbetting.oddsservice.model.outbox.OutboxEventEntity;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publishes a slice of outbox rows to Kafka and marks them published in the same transaction.
 * Invoked once per slice so locks and Kafka coupling stay bounded when polling fallback is enabled.
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
            kafkaTemplate.send(event.getEventType(), event.getPayload());
            event.setPublished(true);
            event.setPublishedAt(publishedAt);
        }
        outboxEventRepository.saveAll(slice);
    }
}
