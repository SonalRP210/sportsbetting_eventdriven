package com.sportsbetting.settlementservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "event_settlements")
public class EventSettlementEntity {
    @Id
    private String eventId;
    @Column(nullable = false)
    private String winningSelection;
    @Column(nullable = false)
    private int winners;
    @Column(nullable = false)
    private int losers;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayout;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getWinningSelection() { return winningSelection; }
    public void setWinningSelection(String winningSelection) { this.winningSelection = winningSelection; }
    public int getWinners() { return winners; }
    public void setWinners(int winners) { this.winners = winners; }
    public int getLosers() { return losers; }
    public void setLosers(int losers) { this.losers = losers; }
    public BigDecimal getTotalPayout() { return totalPayout; }
    public void setTotalPayout(BigDecimal totalPayout) { this.totalPayout = totalPayout; }
}
