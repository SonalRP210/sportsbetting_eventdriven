package com.sportsbetting.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import com.sportsbetting.notificationservice.model.ProcessedEventEntity;
import com.sportsbetting.notificationservice.repository.ProcessedEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventConsumer {
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(
            NotificationService notificationService,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {
            "${app.kafka.topic.betPlaced:betting.bet.placed.v1}",
            "${app.kafka.topic.betCancelled:betting.bet.cancelled.v1}",
            "${app.kafka.topic.eventSettled:settlement.event.settled.v1}",
            "${app.kafka.topic.walletCredited:wallet.wallet.credited.v1}"
    }, groupId = "${app.kafka.group.notification:notification-service}")
    @Transactional
    public void onEvent(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processOnce(topic, payload, () -> {
            Map<String, Object> data = read(payload);
            String eventType = asString(data.getOrDefault("type", data.getOrDefault("eventType", "domain.event")));
            String userId = asString(data.getOrDefault("userId", "system"));
            notificationService.consume(new NotificationEventRequest(eventType, userId, data));
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> read(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid kafka payload", ex);
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void processOnce(String topic, String payload, Runnable handler) {
        String eventKey = topic + ":" + sha256(payload);
        if (processedEventRepository.existsById(eventKey)) {
            return;
        }
        handler.run();
        ProcessedEventEntity processed = new ProcessedEventEntity();
        processed.setEventKey(eventKey);
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash payload", ex);
        }
    }
}
