package com.sportsbetting.betting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "bets", indexes = {
        @Index(name = "idx_bets_user_idempotency", columnList = "userId,idempotencyKey")
})
public class BetEntity {

    @Id
    private String betId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String selection;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal stake;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal odds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetStatus status;

    @Column
    private String idempotencyKey;

    public String getBetId() { return betId; }
    public void setBetId(String betId) { this.betId = betId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSelection() { return selection; }
    public void setSelection(String selection) { this.selection = selection; }
    public BigDecimal getStake() { return stake; }
    public void setStake(BigDecimal stake) { this.stake = stake; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public BetStatus getStatus() { return status; }
    public void setStatus(BetStatus status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
