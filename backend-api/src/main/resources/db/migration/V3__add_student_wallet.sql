-- Student wallet for credit balance
CREATE TABLE student_wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance_cents INTEGER DEFAULT 0 NOT NULL,
    total_top_ups_cents INTEGER DEFAULT 0 NOT NULL,
    total_used_cents INTEGER DEFAULT 0 NOT NULL,
    total_refunded_cents INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL
);

-- Credit transactions history
CREATE TABLE credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES lessons(id) ON DELETE SET NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount_cents INTEGER NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

-- Indexes for faster queries
CREATE INDEX idx_student_wallet_user ON student_wallets(user_id);
CREATE INDEX idx_credit_transaction_user ON credit_transactions(user_id);
CREATE INDEX idx_credit_transaction_lesson ON credit_transactions(lesson_id);
CREATE INDEX idx_credit_transaction_created ON credit_transactions(created_at DESC);
