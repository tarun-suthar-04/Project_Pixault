package com.pixault.model;

import java.time.LocalDateTime;

/**
 * Represents a one-time vault share record.
 */
public class VaultShare {

    private String shareId;           // UUID
    private int ownerId;
    private String encryptedBlobPath;
    private LocalDateTime expiryTime;
    private String deviceFingerprint; // Bound device (nullable before first access)
    private boolean consumed;
    private String hmacSignature;
    private LocalDateTime createdAt;

    public VaultShare() {}

    public VaultShare(String shareId, int ownerId, String encryptedBlobPath,
                      LocalDateTime expiryTime, String hmacSignature) {
        this.shareId = shareId;
        this.ownerId = ownerId;
        this.encryptedBlobPath = encryptedBlobPath;
        this.expiryTime = expiryTime;
        this.hmacSignature = hmacSignature;
        this.consumed = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    // ===== Getters & Setters =====

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getEncryptedBlobPath() { return encryptedBlobPath; }
    public void setEncryptedBlobPath(String encryptedBlobPath) { this.encryptedBlobPath = encryptedBlobPath; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }

    public String getHmacSignature() { return hmacSignature; }
    public void setHmacSignature(String hmacSignature) { this.hmacSignature = hmacSignature; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "VaultShare{shareId='" + shareId + "', ownerId=" + ownerId
                + ", consumed=" + consumed + ", expiryTime=" + expiryTime + "}";
    }
}
