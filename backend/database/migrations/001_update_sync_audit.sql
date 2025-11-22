-- Migration script to update sync_audit table
-- Run this if your database already exists

-- Drop existing sync_audit table and recreate with new schema
DROP TABLE IF EXISTS sync_audit CASCADE;

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

CREATE INDEX idx_sync_audit_user ON sync_audit(user_id, created_at DESC);
