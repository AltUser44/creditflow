-- Run connected to database credit_db as creditflow (or superuser).

CREATE TABLE IF NOT EXISTS credit_scores (
  user_id     VARCHAR(64) PRIMARY KEY,
  score       INTEGER NOT NULL,
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS score_events (
  id          BIGSERIAL PRIMARY KEY,
  user_id     VARCHAR(64) NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_score_events_user_time ON score_events (user_id, occurred_at DESC);
