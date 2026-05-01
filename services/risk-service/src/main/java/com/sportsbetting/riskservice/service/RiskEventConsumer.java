package com.sportsbetting.riskservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.riskservice.dto.BetCancelledEventRequest;
import com.sportsbetting.riskservice.dto.BetPlacedEventRequest;
import com.sportsbetting.riskservice.dto.EventSettledRequest;
import com.sportsbetting.riskservice.model.ProcessedEventEntity;
import com.sportsbetting.riskservice.repository.ProcessedEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class RiskEventConsumer {
    private final RiskService riskService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public RiskEventConsumer(
            RiskService riskService,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.riskService = riskService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.betPlaced:betting.bet.placed.v1}", groupId = "${app.kafka.group.risk:risk-service}")
    @Transactional
    public void onBetPlaced(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processOnce(topic, payload, () -> {
            Map<String, Object> data = read(payload);
            riskService.onBetPlaced(new BetPlacedEventRequest(
                    asString(data.get("betId")),
                    asString(data.get("userId")),
                    asDecimal(data.get("openRisk"))
            ));
        });
    }

    @KafkaListener(topics = "${app.kafka.topic.betCancelled:betting.bet.cancelled.v1}", groupId = "${app.kafka.group.risk:risk-service}")
    @Transactional
    public void onBetCancelled(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processOnce(topic, payload, () -> {
            Map<String, Object> data = read(payload);
            riskService.onBetCancelled(new BetCancelledEventRequest(
                    asString(data.get("betId")),
                    asString(data.get("userId")),
                    asDecimal(data.get("openRisk"))
            ));
        });
    }

    @KafkaListener(topics = "${app.kafka.topic.eventSettled:settlement.event.settled.v1}", groupId = "${app.kafka.group.risk:risk-service}")
    @Transactional
    public void onEventSettled(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processOnce(topic, payload, () -> {
            Map<String, Object> data = read(payload);
            List<EventSettledRequest.RiskRelease> releases = ((List<?>) data.getOrDefault("releases", List.of())).stream()
                    .map(item -> (Map<?, ?>) item)
                    .map(r -> new EventSettledRequest.RiskRelease(asString(r.get("userId")), asDecimal(r.get("openRisk"))))
                    .toList();
            riskService.onEventSettled(new EventSettledRequest(
                    asString(data.get("eventId")),
                    asString(data.get("winningSelection")),
                    releases
            ));
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

    private BigDecimal asDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
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
