-- Video watch progress table for tracking video playback position across devices
CREATE TABLE IF NOT EXISTS video_watch_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    watch_position DOUBLE PRECISION NOT NULL DEFAULT 0,
    duration DOUBLE PRECISION,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_lesson_progress UNIQUE (user_id, lesson_id)
);

-- Index for fast lookups
CREATE INDEX IF NOT EXISTS idx_video_progress_user_lesson ON video_watch_progress(user_id, lesson_id);
