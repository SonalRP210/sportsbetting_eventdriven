package com.sportsbetting.eventingestionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.eventingestionservice.dto.ProviderEventRequest;
import com.sportsbetting.eventingestionservice.model.NormalizedEventEntity;
import com.sportsbetting.eventingestionservice.model.OutboxEventEntity;
import com.sportsbetting.eventingestionservice.repository.NormalizedEventRepository;
import com.sportsbetting.eventingestionservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IngestionService {
  private final NormalizedEventRepository normalizedEventRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public IngestionService(NormalizedEventRepository normalizedEventRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
    this.normalizedEventRepository = normalizedEventRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void ingest(ProviderEventRequest req){
    NormalizedEventEntity event = new NormalizedEventEntity();
    event.setId(UUID.randomUUID().toString());
    event.setProvider(req.provider());
    event.setEventType(req.eventType());
    event.setPayload(writeJson(req.payload()));
    event.setNormalizedAt(Instant.now());
    normalizedEventRepository.save(event);

    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setEventType("ingestion.event.normalized.v1");
    outbox.setPayload(writeJson(Map.of("provider", req.provider(), "eventType", req.eventType(), "payload", req.payload(), "normalizedAt", event.getNormalizedAt().toString())));
    outbox.setPublished(false);
    outbox.setCreatedAt(Instant.now());
    outboxEventRepository.save(outbox);
  }

  @Transactional(readOnly = true)
  public List<Map<String,Object>> events(){
    List<Map<String,Object>> result = new ArrayList<>();
    for (NormalizedEventEntity e : normalizedEventRepository.findAll()) {
      result.add(Map.of("provider", e.getProvider(), "eventType", e.getEventType(), "normalizedAt", e.getNormalizedAt().toString(), "payload", parseJson(e.getPayload())));
    }
    return result;
  }

  @Transactional
  public void resetForTests(){
    outboxEventRepository.deleteAll();
    normalizedEventRepository.deleteAll();
  }

  private String writeJson(Object payload) {
    try { return objectMapper.writeValueAsString(payload); }
    catch (JsonProcessingException e) { throw new IllegalStateException(e); }
  }

  private Object parseJson(String payload) {
    try { return objectMapper.readValue(payload, Object.class); }
    catch (JsonProcessingException e) { throw new IllegalStateException(e); }
  }
}
