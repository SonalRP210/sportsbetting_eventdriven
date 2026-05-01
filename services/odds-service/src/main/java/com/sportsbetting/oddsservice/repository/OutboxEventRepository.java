package com.sportsbetting.oddsservice.repository;

import com.sportsbetting.oddsservice.model.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.createdAt < :cutoff")
    int deleteByCreatedAtBefore(Instant cutoff);
}
