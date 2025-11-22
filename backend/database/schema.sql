-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    is_admin BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Contacts table
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    phone_raw TEXT NOT NULL,
    phone_normalized TEXT NOT NULL UNIQUE,
    created_by UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Call logs table
CREATE TABLE call_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    contact_id UUID REFERENCES contacts(id) ON DELETE SET NULL,
    direction VARCHAR(10) CHECK(direction IN ('incoming', 'outgoing', 'missed')),
    duration_seconds INT,
    timestamp TIMESTAMPTZ DEFAULT now()
);

-- Sync audit table
CREATE TABLE sync_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    sync_type VARCHAR(20) CHECK(sync_type IN ('contacts', 'calls', 'full')),
    status VARCHAR(20) CHECK(status IN ('success', 'error')) NOT NULL,
    error_message TEXT,
    synced_contacts INT DEFAULT 0,
    synced_calls INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE UNIQUE INDEX idx_contacts_phone ON contacts(phone_normalized);
CREATE INDEX idx_contacts_name_trgm ON contacts USING gin (name gin_trgm_ops);
CREATE INDEX idx_call_logs_contact ON call_logs(contact_id);
CREATE INDEX idx_call_logs_timestamp ON call_logs(timestamp DESC);
CREATE INDEX idx_call_logs_user_timestamp ON call_logs(user_id, timestamp DESC);
CREATE INDEX idx_sync_audit_user ON sync_audit(user_id, created_at DESC);
