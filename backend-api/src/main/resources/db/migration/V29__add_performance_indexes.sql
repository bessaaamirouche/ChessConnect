-- Performance indexes for common query patterns
-- These indexes improve query performance for admin dashboards and user lists

-- Composite index for lessons filtered by status and ordered by scheduled_at
-- Used in: findByStatus, admin lesson lists, upcoming lessons
CREATE INDEX IF NOT EXISTS idx_lessons_status_scheduled_at ON lessons(status, scheduled_at DESC);

-- Composite index for lessons by teacher with status
-- Used in: teacher dashboard, lesson history
CREATE INDEX IF NOT EXISTS idx_lessons_teacher_status ON lessons(teacher_id, status);

-- Composite index for lessons by student with status
-- Used in: student dashboard, lesson history
CREATE INDEX IF NOT EXISTS idx_lessons_student_status ON lessons(student_id, status);

-- Index for invoices by customer and date (for user invoice history)
CREATE INDEX IF NOT EXISTS idx_invoices_customer_created ON invoices(customer_id, created_at DESC);

-- Index for payments by status (for admin reconciliation)
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- Index for users by role (for admin user lists)
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Index for availabilities by teacher and date
CREATE INDEX IF NOT EXISTS idx_availabilities_teacher_start ON availabilities(teacher_id, start_time);
