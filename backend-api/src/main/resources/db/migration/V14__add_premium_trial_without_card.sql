-- Add premium trial end date field (trial without credit card)
ALTER TABLE users ADD COLUMN IF NOT EXISTS premium_trial_end DATE;

-- Add index for faster trial lookups
CREATE INDEX IF NOT EXISTS idx_users_premium_trial_end ON users(premium_trial_end) WHERE premium_trial_end IS NOT NULL;

COMMENT ON COLUMN users.premium_trial_end IS 'End date of free premium trial (14 days, no card required)';
