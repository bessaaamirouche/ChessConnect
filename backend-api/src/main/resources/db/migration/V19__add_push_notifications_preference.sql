-- Add push notifications preference to users table
ALTER TABLE users ADD COLUMN push_notifications_enabled BOOLEAN DEFAULT TRUE;
