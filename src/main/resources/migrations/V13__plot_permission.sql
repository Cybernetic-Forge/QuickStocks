-- Add can_manage_plots permission to company_jobs table
ALTER TABLE company_jobs ADD COLUMN can_manage_plots INTEGER NOT NULL DEFAULT 0;
