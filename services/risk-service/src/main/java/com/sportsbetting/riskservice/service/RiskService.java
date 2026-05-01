package com.sportsbetting.riskservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.riskservice.dto.*;
import com.sportsbetting.riskservice.model.*;
import com.sportsbetting.riskservice.repository.OutboxEventRepository;
import com.sportsbetting.riskservice.repository.UserExposureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class RiskService {
    private final UserExposureRepository userExposureRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public RiskService(UserExposureRepository userExposureRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.userExposureRepository = userExposureRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void onBetPlaced(BetPlacedEventRequest event) {
        UserExposureEntity current = userExposureRepository.findById(event.userId()).orElseGet(() -> newExposure(event.userId()));
        current.setOpenRisk(money(current.getOpenRisk().add(money(event.openRisk()))));
        current.setOpenBetCount(current.getOpenBetCount() + 1);
        userExposureRepository.save(current);
        persistOutbox(current, "BET_PLACED");
    }

    @Transactional
    public void onBetCancelled(BetCancelledEventRequest event) {
        UserExposureEntity current = userExposureRepository.findById(event.userId()).orElseGet(() -> newExposure(event.userId()));
        BigDecimal nextRisk = money(current.getOpenRisk().subtract(money(event.openRisk())));
        if (nextRisk.compareTo(BigDecimal.ZERO) < 0) nextRisk = money(BigDecimal.ZERO);
        current.setOpenRisk(nextRisk);
        current.setOpenBetCount(Math.max(current.getOpenBetCount() - 1, 0));
        userExposureRepository.save(current);
        persistOutbox(current, "BET_CANCELLED");
    }

    @Transactional
    public void onEventSettled(EventSettledRequest event) {
        for (EventSettledRequest.RiskRelease release : event.releases()) {
            UserExposureEntity current = userExposureRepository.findById(release.userId()).orElseGet(() -> newExposure(release.userId()));
            BigDecimal nextRisk = money(current.getOpenRisk().subtract(money(release.openRisk())));
            if (nextRisk.compareTo(BigDecimal.ZERO) < 0) nextRisk = money(BigDecimal.ZERO);
            current.setOpenRisk(nextRisk);
            current.setOpenBetCount(Math.max(current.getOpenBetCount() - 1, 0));
            userExposureRepository.save(current);
            persistOutbox(current, "EVENT_SETTLED");
        }
    }

    @Transactional(readOnly = true)
    public UserExposureResponse getUserExposure(String userId) {
        UserExposureEntity e = userExposureRepository.findById(userId).orElseGet(() -> newExposure(userId));
        return new UserExposureResponse(e.getUserId(), money(e.getOpenRisk()), e.getOpenBetCount());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExposure() {
        return money(userExposureRepository.findAll().stream().map(UserExposureEntity::getOpenRisk).reduce(money(BigDecimal.ZERO), BigDecimal::add));
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> outboxEvents() {
        List<DomainEvent> events = new ArrayList<>();
        for (OutboxEventEntity outbox : outboxEventRepository.findAll()) {
            events.add(new DomainEvent(outbox.getEventType(), parsePayload(outbox.getPayload())));
        }
        return events;
    }

    @Transactional
    public void resetForTests() {
        outboxEventRepository.deleteAll();
        userExposureRepository.deleteAll();
    }

    private UserExposureEntity newExposure(String userId) {
        UserExposureEntity e = new UserExposureEntity();
        e.setUserId(userId);
        e.setOpenRisk(money(BigDecimal.ZERO));
        e.setOpenBetCount(0);
        return e;
    }

    private void persistOutbox(UserExposureEntity current, String source) {
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType("risk.exposure.updated.v1");
        outbox.setPayload(writeJson(Map.of("userId", current.getUserId(), "openRisk", current.getOpenRisk(), "openBetCount", current.getOpenBetCount(), "source", source)));
        outbox.setPublished(false);
        outbox.setCreatedAt(Instant.now());
        outboxEventRepository.save(outbox);
    }

    private Map<String, Object> parsePayload(String payload) {
        try { return objectMapper.readValue(payload, LinkedHashMap.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private String writeJson(Map<String, Object> payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
