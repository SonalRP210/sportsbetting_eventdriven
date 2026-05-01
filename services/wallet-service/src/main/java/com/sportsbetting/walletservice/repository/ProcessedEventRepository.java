package com.sportsbetting.walletservice.repository;

import com.sportsbetting.walletservice.model.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
