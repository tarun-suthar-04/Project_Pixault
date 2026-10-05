package com.pixault.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final String SCHEMA = 
        "CREATE TABLE IF NOT EXISTS users (\n" +
        "    id                  INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
        "    username            TEXT NOT NULL UNIQUE,\n" +
        "    email               TEXT NOT NULL UNIQUE,\n" +
        "    password_hash       TEXT NOT NULL,\n" +
        "    salt                TEXT NOT NULL,\n" +
        "    keyfile_hash        TEXT DEFAULT NULL,\n" +
        "    email_verified      INTEGER NOT NULL DEFAULT 0,\n" +
        "    otp_secret          TEXT DEFAULT NULL,\n" +
        "    failed_attempts     INTEGER NOT NULL DEFAULT 0,\n" +
        "    account_locked_until TEXT DEFAULT NULL,\n" +
        "    created_at          TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    updated_at          TEXT NOT NULL DEFAULT (datetime('now'))\n" +
        ");\n" +
        "CREATE INDEX IF NOT EXISTS idx_email ON users(email);\n" +
        "CREATE INDEX IF NOT EXISTS idx_username ON users(username);\n" +
        "\n" +
        "CREATE TABLE IF NOT EXISTS email_tokens (\n" +
        "    id          INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
        "    token_hash  TEXT NOT NULL UNIQUE,\n" +
        "    user_id     INTEGER NOT NULL,\n" +
        "    token_type  TEXT NOT NULL,\n" +
        "    created_at  TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    expires_at  TEXT NOT NULL,\n" +
        "    consumed    INTEGER NOT NULL DEFAULT 0,\n" +
        "    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE\n" +
        ");\n" +
        "CREATE INDEX IF NOT EXISTS idx_token_hash ON email_tokens(token_hash);\n" +
        "CREATE INDEX IF NOT EXISTS idx_user_type ON email_tokens(user_id, token_type);\n" +
        "\n" +
        "CREATE TABLE IF NOT EXISTS otp_codes (\n" +
        "    id          INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
        "    user_id     INTEGER NOT NULL,\n" +
        "    otp_hash    TEXT NOT NULL,\n" +
        "    purpose     TEXT NOT NULL,\n" +
        "    attempts    INTEGER NOT NULL DEFAULT 0,\n" +
        "    created_at  TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    expires_at  TEXT NOT NULL,\n" +
        "    consumed    INTEGER NOT NULL DEFAULT 0,\n" +
        "    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE\n" +
        ");\n" +
        "CREATE INDEX IF NOT EXISTS idx_otp_user ON otp_codes(user_id);\n" +
        "\n" +
        "CREATE TABLE IF NOT EXISTS vault_shares (\n" +
        "    share_id            TEXT NOT NULL PRIMARY KEY,\n" +
        "    owner_id            INTEGER NOT NULL,\n" +
        "    encrypted_blob_path TEXT NOT NULL,\n" +
        "    expiry_time         TEXT NOT NULL,\n" +
        "    device_fingerprint  TEXT DEFAULT NULL,\n" +
        "    consumed            INTEGER NOT NULL DEFAULT 0,\n" +
        "    hmac_signature      TEXT NOT NULL,\n" +
        "    created_at          TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE\n" +
        ");\n" +
        "CREATE INDEX IF NOT EXISTS idx_share_owner ON vault_shares(owner_id);\n" +
        "CREATE INDEX IF NOT EXISTS idx_share_expiry ON vault_shares(expiry_time);\n" +
        "\n" +
        "CREATE TABLE IF NOT EXISTS sessions (\n" +
        "    session_id          TEXT NOT NULL PRIMARY KEY,\n" +
        "    user_id             INTEGER NOT NULL,\n" +
        "    device_fingerprint  TEXT NOT NULL,\n" +
        "    expires_at          TEXT NOT NULL,\n" +
        "    hmac_signature      TEXT NOT NULL,\n" +
        "    revoked             INTEGER NOT NULL DEFAULT 0,\n" +
        "    created_at          TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE\n" +
        ");\n" +
        "CREATE INDEX IF NOT EXISTS idx_session_user ON sessions(user_id);\n" +
        "CREATE INDEX IF NOT EXISTS idx_session_expiry ON sessions(expires_at);\n" +
        "\n" +
        "CREATE TABLE IF NOT EXISTS audit_logs (\n" +
        "    id          INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
        "    user_id     INTEGER DEFAULT NULL,\n" +
        "    event_type  TEXT NOT NULL,\n" +
        "    ip_address  TEXT DEFAULT NULL,\n" +
        "    device_info TEXT DEFAULT NULL,\n" +
        "    details     TEXT DEFAULT NULL,\n" +
        "    timestamp   TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL\n" +
        ");\n" +
        "CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);\n" +
        "CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_logs(event_type);\n" +
        "CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);\n" +
        "\n" +
        "CREATE TABLE IF NOT EXISTS vault_keys (\n" +
        "    user_id             INTEGER NOT NULL PRIMARY KEY,\n" +
        "    encrypted_master_key TEXT NOT NULL,\n" +
        "    key_salt            TEXT NOT NULL,\n" +
        "    key_iv              TEXT NOT NULL,\n" +
        "    created_at          TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    updated_at          TEXT NOT NULL DEFAULT (datetime('now')),\n" +
        "    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE\n" +
        ");";

    public static void initialize() {
        try {
            log.info("Initializing SQLite database...");
            String appDataPath = System.getenv("APPDATA");
            if (appDataPath == null || appDataPath.isBlank()) {
                appDataPath = System.getProperty("user.home");
            }
            
            File pixaultDir = new File(appDataPath, "Pixault");
            if (!pixaultDir.exists()) {
                if (pixaultDir.mkdirs()) {
                    log.info("Created Pixault application data directory at: {}", pixaultDir.getAbsolutePath());
                } else {
                    log.error("Failed to create Pixault application data directory at: {}", pixaultDir.getAbsolutePath());
                }
            }

            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                // SQLite JDBC driver can execute multiple statements separated by ';' via executeUpdate()
                stmt.executeUpdate(SCHEMA);
                log.info("Database schema initialized successfully.");
            }
        } catch (SQLException e) {
            log.error("Failed to initialize database schema: {}", e.getMessage(), e);
        }
    }
}
