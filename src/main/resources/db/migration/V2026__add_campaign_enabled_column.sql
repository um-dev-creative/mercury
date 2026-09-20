-- Migration: add enabled column to campaigns table
-- Run this against the Mercury schema (PostgreSQL)

ALTER TABLE mercury.campaigns
  ADD COLUMN IF NOT EXISTS enabled boolean DEFAULT true;

-- Backfill existing rows if necessary (already defaulted to true)
UPDATE mercury.campaigns SET enabled = true WHERE enabled IS NULL;

