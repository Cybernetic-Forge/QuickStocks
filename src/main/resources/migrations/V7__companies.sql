-- Company / Corporation system

-- Companies table
CREATE TABLE IF NOT EXISTS companies (
  id            TEXT PRIMARY KEY,
  name          TEXT UNIQUE NOT NULL,
  type          TEXT NOT NULL,       -- matches defaultTypes or custom (PRIVATE, PUBLIC, DAO)
  owner_uuid    TEXT NOT NULL,       -- founder UUID
  balance       REAL NOT NULL DEFAULT 0,
  created_at    INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_companies_owner ON companies(owner_uuid);
CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name);

-- Company job titles / roles
CREATE TABLE IF NOT EXISTS company_jobs (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  title         TEXT NOT NULL,
  can_invite    INTEGER NOT NULL DEFAULT 0,    -- SQLite uses INTEGER for BOOLEAN
  can_create_titles INTEGER NOT NULL DEFAULT 0,
  can_withdraw  INTEGER NOT NULL DEFAULT 0,
  can_manage_company INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_company_jobs_unique ON company_jobs(company_id, title);

-- Company employees / members
CREATE TABLE IF NOT EXISTS company_employees (
  company_id    TEXT NOT NULL,
  player_uuid   TEXT NOT NULL,
  job_id        TEXT NOT NULL,
  joined_at     INTEGER NOT NULL,
  PRIMARY KEY (company_id, player_uuid),
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE,
  FOREIGN KEY(job_id) REFERENCES company_jobs(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_company_employees_player ON company_employees(player_uuid);
CREATE INDEX IF NOT EXISTS idx_company_employees_company ON company_employees(company_id);

-- Company transaction history
CREATE TABLE IF NOT EXISTS company_tx (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  player_uuid   TEXT NOT NULL,
  type          TEXT NOT NULL,  -- DEPOSIT | WITHDRAW
  amount        REAL NOT NULL,
  ts            INTEGER NOT NULL,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_company_tx_company ON company_tx(company_id);
CREATE INDEX IF NOT EXISTS idx_company_tx_player ON company_tx(player_uuid);
CREATE INDEX IF NOT EXISTS idx_company_tx_ts ON company_tx(ts);

-- Company invitations
CREATE TABLE IF NOT EXISTS company_invitations (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  inviter_uuid  TEXT NOT NULL,
  invitee_uuid  TEXT NOT NULL,
  job_id        TEXT NOT NULL,
  created_at    INTEGER NOT NULL,
  expires_at    INTEGER NOT NULL,
  status        TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING | ACCEPTED | DECLINED | EXPIRED | CANCELLED
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE,
  FOREIGN KEY(job_id) REFERENCES company_jobs(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_company_invitations_invitee ON company_invitations(invitee_uuid);
CREATE INDEX IF NOT EXISTS idx_company_invitations_company ON company_invitations(company_id);
CREATE INDEX IF NOT EXISTS idx_company_invitations_status ON company_invitations(status);
