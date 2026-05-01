package com.sportsbetting.settlementservice.repository;

import com.sportsbetting.settlementservice.model.BetPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetPositionRepository extends JpaRepository<BetPositionEntity, String> {
    List<BetPositionEntity> findByEventIdAndStatus(String eventId, String status);
}
