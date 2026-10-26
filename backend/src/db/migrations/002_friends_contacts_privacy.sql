-- Migration 002: Friends, Contacts Discovery, Blocked Users, Privacy Settings, and Status Updates

-- 1. Extend Users table with phone number and profile fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(32) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(255) DEFAULT 'Available on ZX Lite';

CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);

-- 2. Friendship Requests table
CREATE TABLE IF NOT EXISTS friendship_requests (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, CANCELLED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_no_self_friend_request CHECK (sender_id != receiver_id)
);

CREATE INDEX IF NOT EXISTS idx_friendship_requests_receiver ON friendship_requests(receiver_id, status);
CREATE INDEX IF NOT EXISTS idx_friendship_requests_sender ON friendship_requests(sender_id, status);

-- 3. Established Friendships table (bidirectional pair stored with canonical user1_id < user2_id)
CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY,
    user1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_friendship_pair UNIQUE (user1_id, user2_id),
    CONSTRAINT check_canonical_order CHECK (user1_id < user2_id)
);

CREATE INDEX IF NOT EXISTS idx_friendships_user1 ON friendships(user1_id);
CREATE INDEX IF NOT EXISTS idx_friendships_user2 ON friendships(user2_id);

-- 4. Blocked Users table
CREATE TABLE IF NOT EXISTS blocked_users (
    id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_blocked_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT check_no_self_block CHECK (blocker_id != blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_blocked_blocker ON blocked_users(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocked_blocked ON blocked_users(blocked_id);

-- 5. User Privacy & Discovery Settings
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    auto_accept_matched_contacts BOOLEAN NOT NULL DEFAULT FALSE,
    allow_phone_discovery BOOLEAN NOT NULL DEFAULT TRUE,
    read_receipts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Ephemeral Status Updates (24-hour expiration)
CREATE TABLE IF NOT EXISTS user_statuses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    text VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_statuses_user_expires ON user_statuses(user_id, expires_at);

-- 7. User FCM Device Tokens for Push Notifications
CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_fcm_user ON user_fcm_tokens(user_id);
