-- V20: Group Lessons support (max 3 students per lesson)

-- Add group fields to lessons table
ALTER TABLE lessons ADD COLUMN is_group_lesson BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE lessons ADD COLUMN max_participants INTEGER DEFAULT 1;
ALTER TABLE lessons ADD COLUMN group_status VARCHAR(20);

-- Lesson participants table (for group lessons)
CREATE TABLE lesson_participants (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    price_paid_cents INTEGER NOT NULL,
    commission_cents INTEGER NOT NULL,
    cancelled_by VARCHAR(20),
    cancelled_at TIMESTAMP,
    refund_percentage INTEGER,
    refunded_amount_cents INTEGER,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (lesson_id, student_id)
);

CREATE INDEX idx_lp_lesson ON lesson_participants(lesson_id);
CREATE INDEX idx_lp_student ON lesson_participants(student_id);
CREATE INDEX idx_lp_status ON lesson_participants(status);

-- Group invitations table
CREATE TABLE group_invitations (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(36) NOT NULL UNIQUE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    max_participants INTEGER NOT NULL DEFAULT 3,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gi_token ON group_invitations(token);
CREATE INDEX idx_gi_lesson ON group_invitations(lesson_id);
