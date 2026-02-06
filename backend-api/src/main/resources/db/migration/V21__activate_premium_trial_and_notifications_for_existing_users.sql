-- Activate 14-day premium trial for all existing students who don't have one yet
UPDATE users
SET premium_trial_end = CURRENT_DATE + INTERVAL '14 days'
WHERE role = 'STUDENT'
  AND premium_trial_end IS NULL;

-- Enable availability notifications for all existing favorite teachers
UPDATE favorite_teachers
SET notify_new_slots = true
WHERE notify_new_slots = false;

-- Add comments for documentation
COMMENT ON COLUMN users.premium_trial_end IS 'End date of free premium trial (14 days, automatically activated for all students)';
