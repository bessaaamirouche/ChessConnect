-- Add recording_segments column to store temporary video segments before concatenation
-- This allows Jibri to send multiple recording webhooks for the same lesson
-- (when participants leave/rejoin the room), and we concatenate them later

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS recording_segments TEXT;

-- Add index for efficient queries on lessons with segments to concatenate
CREATE INDEX IF NOT EXISTS idx_lessons_recording_segments ON lessons(id)
WHERE recording_segments IS NOT NULL AND recording_url IS NULL;

COMMENT ON COLUMN lessons.recording_segments IS 'JSON array of temporary segment URLs before concatenation';
