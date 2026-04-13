CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE boards (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE board_members (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role TEXT NOT NULL,
  CONSTRAINT uq_board_member UNIQUE (board_id, user_id)
);

CREATE TABLE board_columns (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  position INT NOT NULL,
  CONSTRAINT uq_board_column_position UNIQUE (board_id, position)
);

CREATE INDEX idx_board_columns_board_id ON board_columns(board_id);

CREATE TABLE cards (
  id BIGSERIAL PRIMARY KEY,
  column_id BIGINT NOT NULL REFERENCES board_columns(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT NULL,
  assigned_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
  position INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cards_column_id_position ON cards(column_id, position);

CREATE TABLE card_activities (
  id BIGSERIAL PRIMARY KEY,
  card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
  action_type TEXT NOT NULL,
  performed_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
  details TEXT NULL
);

CREATE INDEX idx_card_activities_card_id ON card_activities(card_id);

