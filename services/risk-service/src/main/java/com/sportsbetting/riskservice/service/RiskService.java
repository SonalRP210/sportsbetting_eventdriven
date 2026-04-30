package com.sportsbetting.riskservice.service;

import com.sportsbetting.riskservice.dto.BetCancelledEventRequest;
import com.sportsbetting.riskservice.dto.BetPlacedEventRequest;
import com.sportsbetting.riskservice.dto.EventSettledRequest;
import com.sportsbetting.riskservice.dto.UserExposureResponse;
import com.sportsbetting.riskservice.model.DomainEvent;
import com.sportsbetting.riskservice.model.UserExposure;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RiskService {

    private final Map<String, UserExposure> userExposure = new ConcurrentHashMap<>();
    private final List<DomainEvent> outbox = new ArrayList<>();

    public void onBetPlaced(BetPlacedEventRequest event) {
        UserExposure current = userExposure.getOrDefault(event.userId(), new UserExposure(event.userId(), money(BigDecimal.ZERO), 0));
        UserExposure next = new UserExposure(
                event.userId(),
                money(current.openRisk().add(money(event.openRisk()))),
                current.openBetCount() + 1
        );
        userExposure.put(event.userId(), next);
        outbox.add(new DomainEvent("risk.exposure.updated.v1", Map.of(
                "userId", next.userId(),
                "openRisk", next.openRisk(),
                "openBetCount", next.openBetCount(),
                "source", "BET_PLACED"
        )));
    }

    public void onBetCancelled(BetCancelledEventRequest event) {
        UserExposure current = userExposure.getOrDefault(event.userId(), new UserExposure(event.userId(), money(BigDecimal.ZERO), 0));
        BigDecimal nextRisk = money(current.openRisk().subtract(money(event.openRisk())));
        if (nextRisk.compareTo(BigDecimal.ZERO) < 0) {
            nextRisk = money(BigDecimal.ZERO);
        }
        int nextCount = Math.max(current.openBetCount() - 1, 0);
        UserExposure next = new UserExposure(event.userId(), nextRisk, nextCount);
        userExposure.put(event.userId(), next);
        outbox.add(new DomainEvent("risk.exposure.updated.v1", Map.of(
                "userId", next.userId(),
                "openRisk", next.openRisk(),
                "openBetCount", next.openBetCount(),
                "source", "BET_CANCELLED"
        )));
    }

    public void onEventSettled(EventSettledRequest event) {
        for (EventSettledRequest.RiskRelease release : event.releases()) {
            UserExposure current = userExposure.getOrDefault(release.userId(), new UserExposure(release.userId(), money(BigDecimal.ZERO), 0));
            BigDecimal nextRisk = money(current.openRisk().subtract(money(release.openRisk())));
            if (nextRisk.compareTo(BigDecimal.ZERO) < 0) {
                nextRisk = money(BigDecimal.ZERO);
            }
            int nextCount = Math.max(current.openBetCount() - 1, 0);
            UserExposure next = new UserExposure(release.userId(), nextRisk, nextCount);
            userExposure.put(release.userId(), next);
            outbox.add(new DomainEvent("risk.exposure.updated.v1", Map.of(
                    "userId", next.userId(),
                    "openRisk", next.openRisk(),
                    "openBetCount", next.openBetCount(),
                    "source", "EVENT_SETTLED"
            )));
        }
    }

    public UserExposureResponse getUserExposure(String userId) {
        UserExposure current = userExposure.getOrDefault(userId, new UserExposure(userId, money(BigDecimal.ZERO), 0));
        return new UserExposureResponse(current.userId(), current.openRisk(), current.openBetCount());
    }

    public BigDecimal getTotalExposure() {
        return money(userExposure.values().stream().map(UserExposure::openRisk).reduce(money(BigDecimal.ZERO), BigDecimal::add));
    }

    public List<DomainEvent> outboxEvents() {
        return List.copyOf(outbox);
    }

    public void resetForTests() {
        userExposure.clear();
        outbox.clear();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
