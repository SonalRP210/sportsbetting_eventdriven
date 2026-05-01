package com.sportsbetting.riskservice.repository;

import com.sportsbetting.riskservice.model.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
