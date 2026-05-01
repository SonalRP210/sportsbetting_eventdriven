package com.sportsbetting.eventingestionservice.repository;

import com.sportsbetting.eventingestionservice.model.NormalizedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NormalizedEventRepository extends JpaRepository<NormalizedEventEntity, String> {
}
