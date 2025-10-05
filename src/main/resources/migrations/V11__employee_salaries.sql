-- Employee Salary System

-- Job-level salary configuration
CREATE TABLE IF NOT EXISTS company_job_salaries (
  job_id        TEXT PRIMARY KEY,
  salary_amount REAL NOT NULL DEFAULT 0,
  FOREIGN KEY(job_id) REFERENCES company_jobs(id) ON DELETE CASCADE
);

-- Player-specific salary overrides (takes precedence over job salaries)
CREATE TABLE IF NOT EXISTS company_employee_salaries (
  company_id    TEXT NOT NULL,
  player_uuid   TEXT NOT NULL,
  salary_amount REAL NOT NULL,
  set_at        INTEGER NOT NULL,
  set_by_uuid   TEXT NOT NULL,
  PRIMARY KEY (company_id, player_uuid),
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_company_employee_salaries_player ON company_employee_salaries(player_uuid);

-- Company payment cycle configuration
CREATE TABLE IF NOT EXISTS company_salary_config (
  company_id    TEXT PRIMARY KEY,
  payment_cycle TEXT NOT NULL,  -- 1h, 24h, 1w, 2w, 1m
  last_payment  INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Salary payment history
CREATE TABLE IF NOT EXISTS company_salary_payments (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  player_uuid   TEXT NOT NULL,
  amount        REAL NOT NULL,
  payment_ts    INTEGER NOT NULL,
  cycle         TEXT NOT NULL,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_company_salary_payments_company ON company_salary_payments(company_id);
CREATE INDEX IF NOT EXISTS idx_company_salary_payments_player ON company_salary_payments(player_uuid);
CREATE INDEX IF NOT EXISTS idx_company_salary_payments_ts ON company_salary_payments(payment_ts);

-- Add salaries permission to existing company_jobs table
-- Note: For SQLite, we can't easily add a column with a default, but the column should already exist in new installs
-- For existing databases, we need to handle missing column gracefully in code or use ALTER TABLE
-- Since this is a new column, let's add it
ALTER TABLE company_jobs ADD COLUMN can_manage_salaries INTEGER NOT NULL DEFAULT 0;
