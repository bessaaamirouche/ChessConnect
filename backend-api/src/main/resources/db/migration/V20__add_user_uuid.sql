-- Add UUID column to users table for public profile sharing
ALTER TABLE users ADD COLUMN uuid VARCHAR(36);

-- Generate UUIDs for existing users
UPDATE users SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;

-- Make column non-null and unique
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_uuid_unique UNIQUE (uuid);

-- Add index for fast lookups
CREATE INDEX idx_user_uuid ON users(uuid);
