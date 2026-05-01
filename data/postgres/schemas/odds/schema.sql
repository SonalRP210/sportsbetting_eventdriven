CREATE TABLE IF NOT EXISTS odds.latest_odds (
  event_id VARCHAR(64) NOT NULL,
  selection VARCHAR(64) NOT NULL,
  odds NUMERIC(19,2) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  PRIMARY KEY (event_id, selection)
);
