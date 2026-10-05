package com.pixault.model;

import java.time.LocalDateTime;

/**
 * Represents an authenticated user session.
 * Bound to a specific device fingerprint and signed with HMAC-SHA256.
 */
public class Session {

    private String sessionId;
    private int userId;
    private String username;
    private String deviceFingerprint;
    private LocalDateTime expiresAt;
    private String hmacSignature;
    private boolean revoked;
    private LocalDateTime createdAt;

    public Session() {}

    public Session(String sessionId, int userId, String username,
                   String deviceFingerprint, LocalDateTime expiresAt, String hmacSignature) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.deviceFingerprint = deviceFingerprint;
        this.expiresAt = expiresAt;
        this.hmacSignature = hmacSignature;
        this.revoked = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // ===== Getters & Setters =====

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getHmacSignature() { return hmacSignature; }
    public void setHmacSignature(String hmacSignature) { this.hmacSignature = hmacSignature; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Session{sessionId='" + sessionId + "', userId=" + userId
                + ", username='" + username + "', expiresAt=" + expiresAt + "}";
    }
}
