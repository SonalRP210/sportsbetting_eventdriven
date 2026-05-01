CREATE TABLE IF NOT EXISTS settlement.event_settlements (
  event_id VARCHAR(64) PRIMARY KEY,
  winning_selection VARCHAR(64) NOT NULL,
  winners INT NOT NULL,
  losers INT NOT NULL,
  total_payout NUMERIC(19,2) NOT NULL,
  settled_at TIMESTAMP NOT NULL DEFAULT NOW()
);
