package com.sportsbetting.oddsservice.repository;

import com.sportsbetting.oddsservice.model.odds.OddsQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OddsQuoteRepository extends JpaRepository<OddsQuoteEntity, String> {
    Optional<OddsQuoteEntity> findByEventIdAndSelection(String eventId, String selection);

    List<OddsQuoteEntity> findAllByKeyIdIn(Collection<String> keyIds);
}
