package com.sportsbetting.oddsservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "odds_quotes")
public class OddsQuoteEntity {

    @Id
    private String keyId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String selection;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal odds;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSelection() { return selection; }
    public void setSelection(String selection) { this.selection = selection; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
