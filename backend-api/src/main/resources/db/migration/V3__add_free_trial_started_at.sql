-- Add free_trial_started_at column to persist discovery course timer
-- This ensures the 15-minute countdown continues correctly even if participant leaves and rejoins

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS free_trial_started_at TIMESTAMP;

-- Index for potential queries on free trial lessons
CREATE INDEX IF NOT EXISTS idx_lessons_free_trial ON lessons(is_free_trial) WHERE is_free_trial = true;
