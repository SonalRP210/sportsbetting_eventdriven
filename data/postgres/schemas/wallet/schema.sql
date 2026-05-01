CREATE TABLE IF NOT EXISTS wallet.ledger (
  transaction_id VARCHAR(64) PRIMARY KEY,
  wallet_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  amount NUMERIC(19,2) NOT NULL,
  currency VARCHAR(8) NOT NULL,
  type VARCHAR(32) NOT NULL,
  reference VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
