-- Add thumbnail_url column for video thumbnails in the library
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(512);

-- Add index for faster library queries
CREATE INDEX IF NOT EXISTS idx_lessons_recording_url ON lessons(recording_url) WHERE recording_url IS NOT NULL;
