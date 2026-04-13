CREATE TABLE IF NOT EXISTS board_invites (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  token VARCHAR(64) NOT NULL UNIQUE,
  password_hash TEXT NULL,
  label TEXT NULL,
  expires_at TIMESTAMPTZ NULL,
  created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_board_invites_board_id ON board_invites(board_id);
CREATE INDEX IF NOT EXISTS idx_board_invites_token_active ON board_invites(token) WHERE revoked = FALSE;
