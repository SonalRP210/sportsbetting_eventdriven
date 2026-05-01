package com.sportsbetting.notificationservice.model;

import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModelSmokeTest {

    @Test
    void requestAndEntitiesAccessors() {
        NotificationEventRequest req = new NotificationEventRequest("evt", "u1", Map.of("k", "v"));
        assertThat(req.eventType()).isEqualTo("evt");

        ProcessedEventEntity pe = new ProcessedEventEntity();
        pe.setEventKey("k1");
        pe.setProcessedAt(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(pe.getEventKey()).isEqualTo("k1");

        OutboxEventEntity oe = new OutboxEventEntity();
        oe.setId(UUID.randomUUID());
        oe.setEventType("evt");
        oe.setPayload("{}");
        oe.setPublished(false);
        oe.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
        oe.setPublishedAt(Instant.parse("2020-01-01T00:00:01Z"));
        assertThat(oe.getEventType()).isEqualTo("evt");

        NotificationDeliveryEntity nd = new NotificationDeliveryEntity();
        nd.setId("1");
        nd.setEventType("evt");
        nd.setUserId("u1");
        nd.setStatus("SENT");
        nd.setSentAt(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(nd.getStatus()).isEqualTo("SENT");
    }
}
