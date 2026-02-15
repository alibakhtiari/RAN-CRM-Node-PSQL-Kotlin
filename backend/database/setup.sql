-- RAN-CRM Database Setup (MariaDB)
-- Run this file once to create the schema and seed default users:
--   mysql -u ran_user -p ran_crm < database/setup.sql
-- ============================================================
-- Schema
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    username VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash TEXT NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS contacts (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(255) NOT NULL,
    phone_raw VARCHAR(50) NOT NULL,
    phone_normalized VARCHAR(50) NOT NULL,
    created_by CHAR(36),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    UNIQUE KEY idx_contacts_phone (phone_normalized),
    KEY idx_contacts_created_by (created_by),
    FULLTEXT KEY idx_contacts_name_ft (name),
    CONSTRAINT fk_contacts_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS call_logs (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36),
    contact_id CHAR(36),
    phone_number VARCHAR(50),
    direction VARCHAR(10) CHECK(direction IN ('incoming', 'outgoing', 'missed')),
    duration_seconds INT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_call_logs_contact (contact_id),
    KEY idx_call_logs_timestamp (timestamp),
    KEY idx_call_logs_user_timestamp (user_id, timestamp),
    CONSTRAINT fk_calls_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_calls_contact FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE
    SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sync_audit (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36),
    sync_type VARCHAR(20) CHECK(sync_type IN ('contacts', 'calls', 'full')),
    status VARCHAR(20) NOT NULL CHECK(status IN ('success', 'error')),
    error_message TEXT,
    synced_contacts INT DEFAULT 0,
    synced_calls INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sync_audit_user (user_id, created_at),
    CONSTRAINT fk_sync_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
-- ============================================================
-- Seed Data
-- ============================================================
-- Admin user (username: admin, password: admin123)
INSERT IGNORE INTO users (username, name, password_hash, is_admin)
VALUES (
        'admin',
        'Admin User',
        '$2a$12$CXhuc9o2xuY2xQIJqDwd1.0D3zc.jk6Vrh6GQ6b4xeSVfgxOqBMKO',
        TRUE
    );
-- Normal user (username: user, password: user123)
INSERT IGNORE INTO users (username, name, password_hash, is_admin)
VALUES (
        'user',
        'Normal User',
        '$2a$12$nXvyTzQ/c4qVhz80hfYOkuw0ZjFSbLX6ptbAFUN3ck/SCCxEyScqO',
        FALSE
    );