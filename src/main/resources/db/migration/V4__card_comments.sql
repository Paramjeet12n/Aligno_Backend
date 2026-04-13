CREATE TABLE IF NOT EXISTS card_comments (
  id BIGSERIAL PRIMARY KEY,
  card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  text TEXT NOT NULL,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_card_comments_card_id ON card_comments(card_id);
CREATE INDEX IF NOT EXISTS idx_card_comments_user_id ON card_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_card_comments_card_ts ON card_comments(card_id, timestamp);
