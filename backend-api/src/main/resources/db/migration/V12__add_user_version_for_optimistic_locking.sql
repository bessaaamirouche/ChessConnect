-- Add version column for optimistic locking on User entity
-- This prevents race conditions on fields like hasUsedFreeTrial

ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
