package com.sportsbetting.eventingestionservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "normalized_events")
public class NormalizedEventEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false, length = 8000)
    private String payload;
    @Column(nullable = false)
    private Instant normalizedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getNormalizedAt() { return normalizedAt; }
    public void setNormalizedAt(Instant normalizedAt) { this.normalizedAt = normalizedAt; }
}
