package com.sportsbetting.betting.repository;

import com.sportsbetting.betting.model.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
