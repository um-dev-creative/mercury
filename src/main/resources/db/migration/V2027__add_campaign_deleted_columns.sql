-- Migration: add soft-delete columns to campaigns table
-- Run this against the Mercury schema (PostgreSQL)

ALTER TABLE mercury.campaigns
  ADD COLUMN IF NOT EXISTS deleted boolean DEFAULT false;

ALTER TABLE mercury.campaigns
  ADD COLUMN IF NOT EXISTS deleted_at timestamp;

-- Backfill existing rows if necessary (already defaulted to false)
UPDATE mercury.campaigns SET deleted = false WHERE deleted IS NULL;
