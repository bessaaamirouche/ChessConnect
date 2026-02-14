-- Promo codes & referral system

CREATE TABLE promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    code_type VARCHAR(20) NOT NULL,
    discount_type VARCHAR(30),
    discount_percent DOUBLE PRECISION,
    referrer_name VARCHAR(255),
    referrer_email VARCHAR(255),
    premium_days INTEGER DEFAULT 0,
    revenue_share_percent DOUBLE PRECISION DEFAULT 0,
    max_uses INTEGER,
    current_uses INTEGER DEFAULT 0,
    first_lesson_only BOOLEAN DEFAULT false,
    min_amount_cents INTEGER,
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE promo_code_usages (
    id BIGSERIAL PRIMARY KEY,
    promo_code_id BIGINT NOT NULL REFERENCES promo_codes(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    lesson_id BIGINT REFERENCES lessons(id),
    payment_id BIGINT REFERENCES payments(id),
    original_amount_cents INTEGER NOT NULL,
    discount_amount_cents INTEGER DEFAULT 0,
    commission_saved_cents INTEGER DEFAULT 0,
    used_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE referral_earnings (
    id BIGSERIAL PRIMARY KEY,
    promo_code_id BIGINT NOT NULL REFERENCES promo_codes(id),
    referred_user_id BIGINT NOT NULL REFERENCES users(id),
    lesson_id BIGINT REFERENCES lessons(id),
    lesson_amount_cents INTEGER NOT NULL,
    platform_commission_cents INTEGER NOT NULL,
    referrer_earning_cents INTEGER NOT NULL,
    is_paid BOOLEAN DEFAULT false,
    paid_at TIMESTAMP,
    payment_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Add referral tracking to users
ALTER TABLE users ADD COLUMN referred_by_code_id BIGINT REFERENCES promo_codes(id);
ALTER TABLE users ADD COLUMN referral_code_used_at TIMESTAMP;

-- Add promo tracking to payments
ALTER TABLE payments ADD COLUMN promo_code_id BIGINT REFERENCES promo_codes(id);
ALTER TABLE payments ADD COLUMN discount_amount_cents INTEGER DEFAULT 0;
ALTER TABLE payments ADD COLUMN original_amount_cents INTEGER;

-- Indexes
CREATE INDEX idx_promo_codes_code ON promo_codes(code);
CREATE INDEX idx_promo_codes_type ON promo_codes(code_type);
CREATE INDEX idx_promo_codes_active ON promo_codes(is_active);
CREATE INDEX idx_promo_code_usages_code ON promo_code_usages(promo_code_id);
CREATE INDEX idx_promo_code_usages_user ON promo_code_usages(user_id);
CREATE INDEX idx_referral_earnings_code ON referral_earnings(promo_code_id);
CREATE INDEX idx_referral_earnings_user ON referral_earnings(referred_user_id);
CREATE INDEX idx_referral_earnings_unpaid ON referral_earnings(is_paid) WHERE is_paid = false;
