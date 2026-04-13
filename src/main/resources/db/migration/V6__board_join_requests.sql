CREATE TABLE IF NOT EXISTS board_join_requests (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  invite_id BIGINT NULL REFERENCES board_invites(id) ON DELETE SET NULL,
  status VARCHAR(32) NOT NULL,
  requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  responded_at TIMESTAMPTZ NULL,
  responded_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_bjr_board_status ON board_join_requests(board_id, status);
CREATE INDEX IF NOT EXISTS idx_bjr_user ON board_join_requests(user_id);

-- Allow a user to have at most one pending request per board
CREATE UNIQUE INDEX IF NOT EXISTS uq_bjr_board_user_pending
  ON board_join_requests(board_id, user_id)
  WHERE status = 'PENDING';
