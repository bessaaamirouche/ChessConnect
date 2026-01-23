-- Migration: Remove monthly_quota NOT NULL constraint
-- The subscription model changed from quota-based to Premium-only

-- Allow NULL values for monthly_quota (deprecated column)
ALTER TABLE subscriptions ALTER COLUMN monthly_quota DROP NOT NULL;

-- Also drop lessons_remaining NOT NULL if it exists
ALTER TABLE subscriptions ALTER COLUMN lessons_remaining DROP NOT NULL;
