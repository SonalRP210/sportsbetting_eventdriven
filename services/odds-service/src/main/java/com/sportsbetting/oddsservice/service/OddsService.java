package com.sportsbetting.oddsservice.service;

import com.sportsbetting.oddsservice.model.DomainEvent;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OddsService {

    private final Map<String, BigDecimal> latestOdds = new ConcurrentHashMap<>();
    private final List<DomainEvent> outbox = new ArrayList<>();

    public void consumeOddsFeed(List<OddsUpdate> updates) {
        for (OddsUpdate update : updates) {
            if (update.eventId() == null || update.selection() == null || update.odds() == null || update.odds().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid odds update");
            }
            BigDecimal normalized = money(update.odds());
            latestOdds.put(key(update.eventId(), update.selection()), normalized);
            outbox.add(new DomainEvent("odds.odds.updated.v1", Map.of(
                    "eventId", update.eventId(),
                    "selection", update.selection(),
                    "odds", normalized
            )));
        }
    }

    public Optional<BigDecimal> getOdds(String eventId, String selection) {
        return Optional.ofNullable(latestOdds.get(key(eventId, selection)));
    }

    public List<DomainEvent> outboxEvents() {
        return List.copyOf(outbox);
    }

    public void resetForTests() {
        latestOdds.clear();
        outbox.clear();
    }

    private String key(String eventId, String selection) {
        return eventId + "::" + selection;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
