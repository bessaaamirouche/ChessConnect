-- Web Push Subscriptions table
CREATE TABLE push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint VARCHAR(500) NOT NULL UNIQUE,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP
);

-- Index for fast lookup by user
CREATE INDEX idx_push_subscriptions_user ON push_subscriptions(user_id);

-- Index for fast lookup by endpoint (unique constraint already provides one, but explicit for clarity)
CREATE INDEX idx_push_subscriptions_endpoint ON push_subscriptions(endpoint);
