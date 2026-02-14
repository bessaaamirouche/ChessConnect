ALTER TABLE availabilities ADD COLUMN max_participants INTEGER;
UPDATE availabilities SET max_participants = 2 WHERE lesson_type = 'GROUP' AND max_participants IS NULL;
