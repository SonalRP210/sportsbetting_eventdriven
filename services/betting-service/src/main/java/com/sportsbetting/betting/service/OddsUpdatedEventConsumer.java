package com.sportsbetting.betting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.model.ProcessedEventEntity;
import com.sportsbetting.betting.repository.ProcessedEventRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected collaborators")
public class OddsUpdatedEventConsumer {

    private final BettingService bettingService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public OddsUpdatedEventConsumer(
            BettingService bettingService,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.bettingService = bettingService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper.copy();
    }

    @KafkaListener(
            topics = "${app.kafka.topic.oddsUpdated:odds.updated.v1}",
            groupId = "${app.kafka.group.betting.odds:betting-service-odds}"
    )
    @Transactional
    public void onOddsUpdated(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processOnce(topic, payload, () -> {
            Map<String, Object> data = read(payload);
            // updatedAt is optional in the shared contract until all consumers confirm they read it.
            // The producer always sets it; this guard makes the consumer forward-compatible.
            bettingService.setOdds(
                    asString(data.get("eventId")),
                    asString(data.get("selection")),
                    asDecimal(data.get("odds"))
            );
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


