-- Table for tracking pending course validations for coaches
CREATE TABLE pending_course_validations (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES users(id),
    student_id BIGINT NOT NULL REFERENCES users(id),
    student_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dismissed BOOLEAN DEFAULT FALSE,
    UNIQUE(lesson_id)
);

-- Index for efficient queries by teacher
CREATE INDEX idx_pcv_teacher ON pending_course_validations(teacher_id, dismissed);
