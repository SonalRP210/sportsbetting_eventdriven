CREATE TABLE IF NOT EXISTS betting.bets (
  bet_id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  selection VARCHAR(64) NOT NULL,
  stake NUMERIC(19,2) NOT NULL,
  odds NUMERIC(19,2) NOT NULL,
  status VARCHAR(16) NOT NULL,
  idempotency_key VARCHAR(128),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bet_idempotency ON betting.bets(user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
