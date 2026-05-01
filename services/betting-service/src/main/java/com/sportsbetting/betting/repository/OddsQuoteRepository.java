package com.sportsbetting.betting.repository;

import com.sportsbetting.betting.model.OddsQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OddsQuoteRepository extends JpaRepository<OddsQuoteEntity, String> {
}
