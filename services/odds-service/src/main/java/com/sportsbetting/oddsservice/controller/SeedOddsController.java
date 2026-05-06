package com.sportsbetting.oddsservice.controller;

import com.sportsbetting.oddsservice.api.dto.SeedOddsResponse;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.service.OddsService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dev/test helper: same payload shape as {@code POST /api/v1/odds-feed} for a single update.
 * Disabled when {@code app.features.seed-odds-enabled=false} (production profile default).
 */
@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "app.features.seed-odds-enabled", havingValue = "true")
public class SeedOddsController {

    private final OddsService oddsService;

    public SeedOddsController(OddsService oddsService) {
        this.oddsService = oddsService;
    }

    @PostMapping("/internal/seed-odds")
    public ResponseEntity<SeedOddsResponse> seedOdds(@RequestBody @Valid OddsUpdate request) {
        oddsService.consumeOddsFeed(List.of(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SeedOddsResponse.ok());
    }
}
