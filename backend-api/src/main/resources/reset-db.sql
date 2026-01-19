-- Script to reset database and create admin account
-- Run this manually on the server:
-- docker exec -i chessconnect-db psql -U chess -d chessconnect < backend-api/src/main/resources/reset-db.sql

-- Clear all tables (order matters due to foreign keys)
TRUNCATE TABLE teacher_payouts CASCADE;
TRUNCATE TABLE teacher_balances CASCADE;
TRUNCATE TABLE payments CASCADE;
TRUNCATE TABLE ratings CASCADE;
TRUNCATE TABLE lessons CASCADE;
TRUNCATE TABLE availabilities CASCADE;
TRUNCATE TABLE subscriptions CASCADE;
TRUNCATE TABLE favorite_teachers CASCADE;
TRUNCATE TABLE user_course_progress CASCADE;
TRUNCATE TABLE progress_tracking CASCADE;
TRUNCATE TABLE quiz_results CASCADE;
TRUNCATE TABLE password_reset_tokens CASCADE;
TRUNCATE TABLE users CASCADE;

-- Note: Admin account will be created automatically by AdminDataInitializer on next backend startup
