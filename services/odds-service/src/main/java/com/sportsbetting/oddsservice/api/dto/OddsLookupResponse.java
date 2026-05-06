package com.sportsbetting.oddsservice.api.dto;

/**
 * Success ({@link OddsQuoteResponse}) or not-found ({@link OddsNotFoundResponse}) body for odds lookup.
 */
public sealed interface OddsLookupResponse permits OddsQuoteResponse, OddsNotFoundResponse {
}
