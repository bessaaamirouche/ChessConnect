-- Add teacher comment field for coaches to leave feedback to students
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS teacher_comment TEXT;
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS teacher_comment_at TIMESTAMP;

-- Add soft delete fields - allows coach/student to hide lesson from their history
-- while keeping data intact for the other party
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS deleted_by_teacher BOOLEAN DEFAULT FALSE;
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS deleted_by_student BOOLEAN DEFAULT FALSE;

-- Index for faster history queries with soft delete
CREATE INDEX IF NOT EXISTS idx_lesson_deleted_by_teacher ON lessons(teacher_id, deleted_by_teacher);
CREATE INDEX IF NOT EXISTS idx_lesson_deleted_by_student ON lessons(student_id, deleted_by_student);
