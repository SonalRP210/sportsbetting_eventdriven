package com.sportsbetting.riskservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "user_exposure")
public class UserExposureEntity {
    @Id
    private String userId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal openRisk;
    @Column(nullable = false)
    private int openBetCount;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getOpenRisk() { return openRisk; }
    public void setOpenRisk(BigDecimal openRisk) { this.openRisk = openRisk; }
    public int getOpenBetCount() { return openBetCount; }
    public void setOpenBetCount(int openBetCount) { this.openBetCount = openBetCount; }
}
