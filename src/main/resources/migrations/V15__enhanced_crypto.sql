-- Enhanced Crypto Support
-- Adds company ownership for cryptocurrencies and creation tracking

-- Add company_id column to instruments table to support company-owned crypto
ALTER TABLE instruments ADD COLUMN company_id TEXT;

-- Create index for company-owned instruments lookups
CREATE INDEX IF NOT EXISTS idx_instruments_company_id ON instruments(company_id);

-- Add foreign key constraint if companies table exists (will fail gracefully if not)
-- Note: SQLite doesn't support ALTER TABLE ADD CONSTRAINT, so this is a no-op in SQLite
-- For MySQL/PostgreSQL, this can be added in future migrations if needed
