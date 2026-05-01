package com.sportsbetting.betting.service;

import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OutboxDispatcher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectProvider<EventPayloadValidator> eventSchemaValidator;

    public OutboxDispatcher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectProvider<EventPayloadValidator> eventSchemaValidator
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventSchemaValidator = eventSchemaValidator;
    }

    @Transactional
    public int dispatchPending() {
        int sent = 0;
        for (OutboxEventEntity event : outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()) {
            eventSchemaValidator.ifAvailable(v -> v.validateIfPresent(event.getEventType(), event.getPayload()));
            kafkaTemplate.send(event.getEventType(), event.getPayload());
            event.setPublished(true);
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
            sent++;
        }
        return sent;
    }
}
