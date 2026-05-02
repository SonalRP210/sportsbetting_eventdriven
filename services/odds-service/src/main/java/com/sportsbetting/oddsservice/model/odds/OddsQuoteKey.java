package com.sportsbetting.oddsservice.model.odds;

/**
 * Stable composite identifier for an odds quote row ({@code odds.odds_quotes.key_id}).
 */
public record OddsQuoteKey(String eventId, String selection) {

    public static OddsQuoteKey from(OddsUpdate update) {
        return new OddsQuoteKey(update.eventId(), update.selection());
    }

    public String asString() {
        return eventId + "::" + selection;
    }
}
