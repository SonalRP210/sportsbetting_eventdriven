package com.sportsbetting.oddsservice.repository;

import com.sportsbetting.oddsservice.model.OddsQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OddsQuoteRepository extends JpaRepository<OddsQuoteEntity, String> {
    Optional<OddsQuoteEntity> findByEventIdAndSelection(String eventId, String selection);
}
