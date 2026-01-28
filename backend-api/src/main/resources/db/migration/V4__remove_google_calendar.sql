-- Remove Google Calendar integration columns from users table
ALTER TABLE users DROP COLUMN IF EXISTS google_calendar_token;
ALTER TABLE users DROP COLUMN IF EXISTS google_calendar_refresh_token;
ALTER TABLE users DROP COLUMN IF EXISTS google_calendar_enabled;
