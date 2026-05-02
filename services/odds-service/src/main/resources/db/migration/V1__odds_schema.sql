CREATE SCHEMA IF NOT EXISTS odds;

CREATE TABLE IF NOT EXISTS odds.odds_quotes (
    key_id VARCHAR(255) NOT NULL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    selection VARCHAR(255) NOT NULL,
    odds NUMERIC(19, 2) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_odds_quotes_event_selection ON odds.odds_quotes (event_id, selection);

CREATE TABLE IF NOT EXISTS odds.outbox_events (
    id UUID NOT NULL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    payload VARCHAR(8000) NOT NULL,
    published BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_pending_created ON odds.outbox_events (created_at ASC)
    WHERE published = false;

CREATE INDEX IF NOT EXISTS idx_outbox_events_created_at ON odds.outbox_events (created_at);
