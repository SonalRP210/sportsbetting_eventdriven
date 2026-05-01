package com.sportsbetting.walletservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.walletservice.dto.SettlementEventRequest;
import com.sportsbetting.walletservice.dto.SettlementRelease;
import com.sportsbetting.walletservice.model.ProcessedEventEntity;
import com.sportsbetting.walletservice.repository.ProcessedEventRepository;
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
public class WalletEventConsumer {
    private final WalletService walletService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public WalletEventConsumer(
            WalletService walletService,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.walletService = walletService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.eventSettled:settlement.event.settled.v1}", groupId = "${app.kafka.group.wallet:wallet-service}")
    @Transactional
    public void onEventSettled(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processOnce(topic, payload, () -> {
            Map<String, Object> data = read(payload);
            List<SettlementRelease> releases = ((List<?>) data.getOrDefault("releases", List.of())).stream()
                    .map(item -> (Map<?, ?>) item)
                    .map(r -> new SettlementRelease(asString(r.get("userId")), asDecimal(r.get("openRisk"))))
                    .toList();
            walletService.consumeSettlementEvent(new SettlementEventRequest(
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
