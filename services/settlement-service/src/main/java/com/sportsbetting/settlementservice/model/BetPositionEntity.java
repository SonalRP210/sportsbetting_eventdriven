package com.sportsbetting.settlementservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "bet_positions")
public class BetPositionEntity {
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
    @Column(nullable = false)
    private String status;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
