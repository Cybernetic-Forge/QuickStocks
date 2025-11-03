-- Add plot-specific permissions table
CREATE TABLE IF NOT EXISTS plot_permissions (
  id            TEXT PRIMARY KEY,
  plot_id       TEXT NOT NULL,
  job_id        TEXT NOT NULL,
  can_build     INTEGER NOT NULL DEFAULT 1,
  can_interact  INTEGER NOT NULL DEFAULT 1,
  can_container INTEGER NOT NULL DEFAULT 1,
  FOREIGN KEY(plot_id) REFERENCES company_plots(id) ON DELETE CASCADE,
  FOREIGN KEY(job_id) REFERENCES company_jobs(id) ON DELETE CASCADE,
  UNIQUE(plot_id, job_id)
);

CREATE INDEX IF NOT EXISTS idx_plot_permissions_plot ON plot_permissions(plot_id);
CREATE INDEX IF NOT EXISTS idx_plot_permissions_job ON plot_permissions(job_id);

-- Add default plot permissions configuration to companies
-- This will be stored as JSON in a new column
ALTER TABLE companies ADD COLUMN default_plot_permissions TEXT DEFAULT NULL;
