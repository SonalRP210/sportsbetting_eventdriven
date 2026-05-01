package com.sportsbetting.settlementservice.repository;

import com.sportsbetting.settlementservice.model.EventSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSettlementRepository extends JpaRepository<EventSettlementEntity, String> {
}
