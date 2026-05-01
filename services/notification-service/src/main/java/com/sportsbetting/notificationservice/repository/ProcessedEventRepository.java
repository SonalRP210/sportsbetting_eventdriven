package com.sportsbetting.notificationservice.repository;

import com.sportsbetting.notificationservice.model.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
