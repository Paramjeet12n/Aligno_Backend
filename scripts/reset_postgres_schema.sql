-- =============================================================================
-- RealTimeBoard – PostgreSQL / Neon: wipe app schema and rebuild
--
-- Matches Flyway migrations: V1__init.sql + V2__board_invites.sql + V3__user_app_role.sql
-- (users.app_role merged into CREATE users below).
--
-- OPTION A – Recommended if the app runs with Flyway enabled
--   1. Run ONLY the section "1) DROP ALL OBJECTS" below (through flyway_schema_history).
--   2. Restart Spring Boot. Flyway will re-apply V1, V2, V3 and recreate everything.
--
-- OPTION B – Full reset in one editor session (no Flyway on next start, or baseline manually)
--   1. Run "1) DROP ALL OBJECTS".
--   2. Run "2) CREATE SCHEMA" in the same session.
--   3. Set spring.flyway.enabled=false until you align Flyway history, OR delete rows from
--      flyway_schema_history and let Flyway baseline (advanced). Easiest: use OPTION A.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) DROP ALL OBJECTS (children first; CASCADE handles stray deps)
-- -----------------------------------------------------------------------------

DROP TABLE IF EXISTS card_activities CASCADE;
DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS board_invites CASCADE;
DROP TABLE IF EXISTS board_columns CASCADE;
DROP TABLE IF EXISTS board_members CASCADE;
DROP TABLE IF EXISTS boards CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- So Flyway can re-run V1–V3 after OPTION A
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- ############# STOP HERE FOR OPTION A — restart Spring Boot (Flyway rebuilds DB) #############
-- Do not run section 2 below unless you want a fully manual schema (then disable Flyway or baseline).

-- -----------------------------------------------------------------------------
-- 2) CREATE SCHEMA (OPTION B only – skip if you chose OPTION A and will restart the app)
-- -----------------------------------------------------------------------------

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  app_role VARCHAR(32) NOT NULL DEFAULT 'USER'
);

CREATE TABLE boards (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE board_members (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  role TEXT NOT NULL,
  CONSTRAINT uq_board_member UNIQUE (board_id, user_id)
);

CREATE TABLE board_columns (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  position INT NOT NULL,
  CONSTRAINT uq_board_column_position UNIQUE (board_id, position)
);

CREATE INDEX idx_board_columns_board_id ON board_columns (board_id);

CREATE TABLE cards (
  id BIGSERIAL PRIMARY KEY,
  column_id BIGINT NOT NULL REFERENCES board_columns (id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT NULL,
  assigned_user_id BIGINT NULL REFERENCES users (id) ON DELETE SET NULL,
  position INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cards_column_id_position ON cards (column_id, position);

CREATE TABLE card_activities (
  id BIGSERIAL PRIMARY KEY,
  card_id BIGINT NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
  action_type TEXT NOT NULL,
  performed_by BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
  details TEXT NULL
);

CREATE INDEX idx_card_activities_card_id ON card_activities (card_id);

CREATE TABLE board_invites (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards (id) ON DELETE CASCADE,
  token VARCHAR(64) NOT NULL UNIQUE,
  password_hash TEXT NULL,
  label TEXT NULL,
  expires_at TIMESTAMPTZ NULL,
  created_by BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_board_invites_board_id ON board_invites (board_id);
CREATE INDEX idx_board_invites_token_active ON board_invites (token) WHERE revoked = FALSE;
