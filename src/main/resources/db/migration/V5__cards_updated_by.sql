ALTER TABLE cards
  ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_cards_updated_by_user_id ON cards(updated_by_user_id);
