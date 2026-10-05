-- ============================================================
-- Pixault Authentication System Upgrade
-- Migration Script
-- ============================================================

USE pixault_db;

-- 1. Update users table for better rate limiting and lockouts
ALTER TABLE users 
ADD COLUMN failed_login_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER otp_secret,
ADD COLUMN login_locked_until DATETIME DEFAULT NULL AFTER failed_login_attempts;

-- 2. Drop the old token and otp tables to refactor
DROP TABLE IF EXISTS email_tokens;
DROP TABLE IF EXISTS otp_codes;

-- 3. Create a unified, secure otp_codes table
CREATE TABLE otp_codes (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         INT UNSIGNED NOT NULL,
    otp_hash        VARCHAR(128) NOT NULL,                   -- SHA-256 of 6-digit OTP
    purpose         ENUM('EMAIL_VERIFY', 'FORGOT_PASSWORD') NOT NULL,
    expires_at      DATETIME NOT NULL,
    attempts        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_until   DATETIME DEFAULT NULL,                   -- Rate limit for this specific purpose/user
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_otp_user_purpose (user_id, purpose),
    INDEX idx_otp_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Create or update sessions table for revocation support
CREATE TABLE IF NOT EXISTS sessions (
    session_id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id             INT UNSIGNED NOT NULL,
    device_fingerprint  VARCHAR(256) NOT NULL,
    expires_at          DATETIME     NOT NULL,
    hmac_signature      VARCHAR(256) NOT NULL,
    revoked             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_session_user (user_id),
    INDEX idx_session_expiry (expires_at),
    INDEX idx_session_revoked (revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
