-- Run connected to database transactions_db as creditflow (or superuser).

CREATE TABLE IF NOT EXISTS transactions (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     VARCHAR(64) NOT NULL,
  amount      NUMERIC(18, 2) NOT NULL,
  txn_type    VARCHAR(16) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
