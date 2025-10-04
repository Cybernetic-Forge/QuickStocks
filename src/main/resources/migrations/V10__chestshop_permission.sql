-- Add ChestShop management permission to company jobs
-- This permission allows employees to manage chest shops owned by their company

ALTER TABLE company_jobs ADD COLUMN can_manage_chestshop INTEGER NOT NULL DEFAULT 0;
