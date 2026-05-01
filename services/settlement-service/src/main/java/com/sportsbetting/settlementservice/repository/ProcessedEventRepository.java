package com.sportsbetting.settlementservice.repository;

import com.sportsbetting.settlementservice.model.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
