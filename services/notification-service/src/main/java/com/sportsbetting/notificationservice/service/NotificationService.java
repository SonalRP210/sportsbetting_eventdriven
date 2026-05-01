package com.sportsbetting.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import com.sportsbetting.notificationservice.model.NotificationDeliveryEntity;
import com.sportsbetting.notificationservice.model.OutboxEventEntity;
import com.sportsbetting.notificationservice.repository.NotificationDeliveryRepository;
import com.sportsbetting.notificationservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class NotificationService {
  private final NotificationDeliveryRepository deliveryRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public NotificationService(NotificationDeliveryRepository deliveryRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
    this.deliveryRepository = deliveryRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void consume(NotificationEventRequest req){
    NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
    delivery.setId(UUID.randomUUID().toString());
    delivery.setEventType(req.eventType());
    delivery.setUserId(req.userId());
    delivery.setStatus("SENT");
    delivery.setSentAt(Instant.now());
    deliveryRepository.save(delivery);

    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setEventType("notification.delivery.sent.v1");
    outbox.setPayload(writeJson(Map.of("eventType", req.eventType(), "userId", req.userId(), "status", "SENT", "sentAt", delivery.getSentAt().toString())));
    outbox.setPublished(false);
    outbox.setCreatedAt(Instant.now());
    outboxEventRepository.save(outbox);
  }

  @Transactional(readOnly = true)
  public List<Map<String,Object>> deliveries(){
    List<Map<String,Object>> result = new ArrayList<>();
    for (NotificationDeliveryEntity d : deliveryRepository.findAll()) {
      result.add(Map.of("eventType", d.getEventType(), "userId", d.getUserId(), "status", d.getStatus(), "sentAt", d.getSentAt().toString()));
    }
    return result;
  }

  @Transactional
  public void resetForTests(){
    outboxEventRepository.deleteAll();
    deliveryRepository.deleteAll();
  }

  private String writeJson(Map<String, Object> payload) {
    try { return objectMapper.writeValueAsString(payload); }
    catch (JsonProcessingException e) { throw new IllegalStateException(e); }
  }
}
