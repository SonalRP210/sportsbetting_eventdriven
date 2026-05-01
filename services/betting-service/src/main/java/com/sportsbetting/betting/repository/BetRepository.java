package com.sportsbetting.betting.repository;

import com.sportsbetting.betting.model.BetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BetRepository extends JpaRepository<BetEntity, String> {
    Optional<BetEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    List<BetEntity> findByUserIdOrderByBetIdAsc(String userId);
    List<BetEntity> findByEventIdOrderByBetIdAsc(String eventId);
}
