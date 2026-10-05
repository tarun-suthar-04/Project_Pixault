-- ============================================================
-- Pixault – Secure Steganography & Encrypted Digital Vault
-- SQLite Schema
-- ============================================================

-- ============================================================
-- Table: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    username            TEXT NOT NULL UNIQUE,
    email               TEXT NOT NULL UNIQUE,
    password_hash       TEXT NOT NULL,
    salt                TEXT NOT NULL,
    keyfile_hash        TEXT DEFAULT NULL,
    email_verified      INTEGER NOT NULL DEFAULT 0,
    otp_secret          TEXT DEFAULT NULL,
    failed_attempts     INTEGER NOT NULL DEFAULT 0,
    account_locked_until TEXT DEFAULT NULL,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at          TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_username ON users(username);

-- ============================================================
-- Table: email_tokens  (verification & password reset share structure)
-- ============================================================
CREATE TABLE IF NOT EXISTS email_tokens (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    token_hash  TEXT NOT NULL UNIQUE,
    user_id     INTEGER NOT NULL,
    token_type  TEXT NOT NULL,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    expires_at  TEXT NOT NULL,
    consumed    INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_token_hash ON email_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_user_type ON email_tokens(user_id, token_type);

-- ============================================================
-- Table: otp_codes   (Email OTP – short-lived 6-digit codes)
-- ============================================================
CREATE TABLE IF NOT EXISTS otp_codes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    otp_hash    TEXT NOT NULL,
    purpose     TEXT NOT NULL,
    attempts    INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    expires_at  TEXT NOT NULL,
    consumed    INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_otp_user ON otp_codes(user_id);

-- ============================================================
-- Table: vault_shares  (One-time encrypted sharing)
-- ============================================================
CREATE TABLE IF NOT EXISTS vault_shares (
    share_id            TEXT NOT NULL PRIMARY KEY,
    owner_id            INTEGER NOT NULL,
    encrypted_blob_path TEXT NOT NULL,
    expiry_time         TEXT NOT NULL,
    device_fingerprint  TEXT DEFAULT NULL,
    consumed            INTEGER NOT NULL DEFAULT 0,
    hmac_signature      TEXT NOT NULL,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_share_owner ON vault_shares(owner_id);
CREATE INDEX IF NOT EXISTS idx_share_expiry ON vault_shares(expiry_time);

-- ============================================================
-- Table: sessions  (Active authenticated sessions)
-- ============================================================
CREATE TABLE IF NOT EXISTS sessions (
    session_id          TEXT NOT NULL PRIMARY KEY,
    user_id             INTEGER NOT NULL,
    device_fingerprint  TEXT NOT NULL,
    expires_at          TEXT NOT NULL,
    hmac_signature      TEXT NOT NULL,
    revoked             INTEGER NOT NULL DEFAULT 0,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_session_user ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_session_expiry ON sessions(expires_at);

-- ============================================================
-- Table: audit_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER DEFAULT NULL,
    event_type  TEXT NOT NULL,
    ip_address  TEXT DEFAULT NULL,
    device_info TEXT DEFAULT NULL,
    details     TEXT DEFAULT NULL,
    timestamp   TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);

-- ============================================================
-- Table: vault_keys  (Encrypted master key per user)
-- ============================================================
CREATE TABLE IF NOT EXISTS vault_keys (
    user_id             INTEGER NOT NULL PRIMARY KEY,
    encrypted_master_key TEXT NOT NULL,
    key_salt            TEXT NOT NULL,
    key_iv              TEXT NOT NULL,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at          TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
