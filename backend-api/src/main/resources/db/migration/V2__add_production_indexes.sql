-- Production performance indexes
-- These indexes optimize common queries for better performance

-- Payments table indexes
CREATE INDEX IF NOT EXISTS idx_payments_payer_id ON payments(payer_id);
CREATE INDEX IF NOT EXISTS idx_payments_teacher_id ON payments(teacher_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_type_status ON payments(payment_type, status);

-- Invoices table indexes
CREATE INDEX IF NOT EXISTS idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_issuer_id ON invoices(issuer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_issued_at ON invoices(issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_type ON invoices(invoice_type);

-- Subscriptions table indexes
CREATE INDEX IF NOT EXISTS idx_subscriptions_student_id ON subscriptions(student_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_active ON subscriptions(is_active);
CREATE INDEX IF NOT EXISTS idx_subscriptions_end_date ON subscriptions(end_date);

-- Credit transactions table indexes
CREATE INDEX IF NOT EXISTS idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_created_at ON credit_transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_type ON credit_transactions(transaction_type);

-- Availabilities table indexes
CREATE INDEX IF NOT EXISTS idx_availabilities_teacher_id ON availabilities(teacher_id);
CREATE INDEX IF NOT EXISTS idx_availabilities_specific_date ON availabilities(specific_date);
CREATE INDEX IF NOT EXISTS idx_availabilities_day_of_week ON availabilities(day_of_week);

-- Ratings table indexes
CREATE INDEX IF NOT EXISTS idx_ratings_teacher_id ON ratings(teacher_id);
CREATE INDEX IF NOT EXISTS idx_ratings_student_id ON ratings(student_id);
CREATE INDEX IF NOT EXISTS idx_ratings_created_at ON ratings(created_at DESC);

-- Articles table indexes (for blog)
CREATE INDEX IF NOT EXISTS idx_articles_published_at ON articles(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_articles_category ON articles(category);
CREATE INDEX IF NOT EXISTS idx_articles_published ON articles(published);

-- Quiz results indexes
CREATE INDEX IF NOT EXISTS idx_quiz_results_student_id ON quiz_results(student_id);
CREATE INDEX IF NOT EXISTS idx_quiz_results_completed_at ON quiz_results(completed_at DESC);

-- Teacher payouts indexes
CREATE INDEX IF NOT EXISTS idx_teacher_payouts_teacher_id ON teacher_payouts(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_payouts_year_month ON teacher_payouts(year_month);
CREATE INDEX IF NOT EXISTS idx_teacher_payouts_paid ON teacher_payouts(is_paid);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_lessons_teacher_date_status ON lessons(teacher_id, scheduled_at, status);
CREATE INDEX IF NOT EXISTS idx_lessons_student_date_status ON lessons(student_id, scheduled_at, status);
CREATE INDEX IF NOT EXISTS idx_payments_payer_date ON payments(payer_id, created_at DESC);

-- User course progress indexes
CREATE INDEX IF NOT EXISTS idx_user_course_progress_user ON user_course_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_user_course_progress_course ON user_course_progress(course_id);
CREATE INDEX IF NOT EXISTS idx_user_course_progress_status ON user_course_progress(status);

-- Partial index for active lessons only
CREATE INDEX IF NOT EXISTS idx_lessons_active ON lessons(scheduled_at, teacher_id)
WHERE status IN ('PENDING', 'CONFIRMED');
